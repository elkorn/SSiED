package com.rapidminer;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.AttributeValueFilter;
import com.rapidminer.example.set.AttributeValueFilterSingleCondition;
import com.rapidminer.example.set.ConditionedExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.example.Attribute;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.operator.preprocessing.join.ExampleSetMerge;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.math.container.GeometricDataCollection;
import com.rapidminer.tools.math.container.LinearList;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.DistanceMeasureHelper;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.List;
import java.util.LinkedList;


public class Smote extends Operator{

    public static final String PARAMETER_SAMPLE_SIZE_PER_CLASS_LIST = "sample_size_per_class";
    public static final String PARAMETER_SMOTE_N = "smote_n";
    public static final String PARAMETER_MINORITY_CLASS_NAME = "minority_class_name";
    public static final String PARAMETER_NEAREST_NEIGHBORS = "nearest_neighbors";
    private final InputPort exampleSetInput = getInputPorts().createPort("example set input");
    private final OutputPort exampleSetOutput = getOutputPorts().createPort("example set output");
    private final OutputPort originalOutput = getOutputPorts().createPort("original");
    private DistanceMeasureHelper measureHelper = new DistanceMeasureHelper(this);
    
	public Smote(OperatorDescription description){
		super(description);
		
		exampleSetInput.addPrecondition(new SimplePrecondition(exampleSetInput, getRequiredMetaData()));
        getTransformer().addPassThroughRule(exampleSetInput, originalOutput); 
        getTransformer().addRule(new PassThroughRule(exampleSetInput, exampleSetOutput, false) {
        	
        	@Override
            public MetaData modifyMetaData(MetaData metaData) {
        		if (metaData instanceof ExampleSetMetaData) {
                        ExampleSetMetaData exampleSetMetaData = ((ExampleSetMetaData)metaData);
                        
                        exampleSetMetaData.setNumberOfExamples(exampleSetMetaData.getNumberOfExamples());
                } else {
                    return metaData;
                }
                return metaData;
            }
        	
        });
	}
	
	/**
	 * Creates required meta data object.
	 * @return ExampleSet meta data
	 */
    protected ExampleSetMetaData getRequiredMetaData() {
        return new ExampleSetMetaData();
    }
	
    /**
     * Performs actual data balancing.
     * @param originalSet - original example set from operator input
     * @return balanced example set
     * @throws OperatorException
     */
	private ExampleSet apply(ExampleSet originalSet) throws OperatorException {
		
		// TODO: Handling n<100 (if it's really necessary in our case)
		Integer n = getParameterAsInt(PARAMETER_SMOTE_N) / 100;
		Integer k = getParameterAsInt(PARAMETER_NEAREST_NEIGHBORS);
		String minorityClass = getParameterAsString(PARAMETER_MINORITY_CLASS_NAME);
		
        Attribute label = null;
        AttributeValueFilter filter = null;
        ConditionedExampleSet minoritySet = null;
        SplittedExampleSet perLabelSets = null;
        MemoryExampleTable smotedTable = MemoryExampleTable.createCompleteCopy(originalSet.getExampleTable());
        int resultSize = 0;
        
        label = originalSet.getAttributes().getLabel();
        if (label != null) {
        	if (label.isNominal()) {
        		filter = new AttributeValueFilter(label, AttributeValueFilterSingleCondition.EQUALS, minorityClass);
        		minoritySet = new ConditionedExampleSet(originalSet, filter);
        	} 
            else {
            	// TODO: Magic number?
                throw new UserError(this, 105);
            }
        } 
        else {
            throw new UserError(this, resultSize);
        }
        
        ExampleTable minorityTable = minoritySet.getExampleTable();
        Integer t = minorityTable.size();
        
        DistanceMeasure measure = measureHelper.getInitializedMeasure(minoritySet);
    	
    	GeometricDataCollection<double[]> samples = new LinearList<double[]>(measure);

		Attributes attributes = minoritySet.getAttributes();

		int valuesSize = attributes.size();
		for(Example example: minoritySet) {
			double[] values = new double[valuesSize];
			int i = 0;
			for (Attribute attribute: attributes) {
				values[i] = example.getValue(attribute);
				i++;
			}
			samples.add(values, values);
			checkForStop();
		}
        
        for (int i=0; i<t; i++) {
        	DataRow example = minorityTable.getDataRow(i);
        	int index = 0;
        	
        	double[] values = new double[valuesSize];
        	for (Attribute attribute: attributes) {
				values[index] = example.get(attribute);
				index++;
			}
        	
        	// calculate k nearest neighbors
        	Collection<double[]> neighborValues = samples.getNearestValues(k, values);
        	// I hate Java
        	Iterator<double[]> it = neighborValues.iterator();
        	double[][] neighborArray = new double[neighborValues.size()][];
        	
        	index = 0;
        	while (it.hasNext()) {
        		neighborArray[index] = it.next();
        		index++;
        	}
        	Random generator = new Random();
        	
        	// generate synthetic examples
        	for (int j=0; j<n; j++) {
        		double[] synthetic = new double[valuesSize];
        		// get random neighbor
        		double[] randomNeighborValues = neighborArray[generator.nextInt(neighborValues.size() - 1)];
        		// for each continuous attribute
        		index = 0;
        		for (Attribute attribute: attributes) {
        			// calculate distance
    				double distance = Math.abs(randomNeighborValues[index] - values[index]);
    				// generate random gap
    				double gap = generator.nextDouble();
    				// attr = sample attr + distance * gap
    				double newValue = values[index] + distance * gap;
    				synthetic[index] = newValue;
    				index++;
    				smotedTable.addDataRow(new DoubleArrayDataRow(synthetic));
    			}
        	}

			checkForStop();
        }
        
        return smotedTable.createExampleSet();
		
		// old code
		
//		ExampleSetMerge mergeOperator;
//		try {
//			mergeOperator = OperatorService.createOperator(ExampleSetMerge.class);
//		} catch (OperatorCreationException e) {
//			e.printStackTrace();
//			return originalSet;
//		}
//
//		//SplittedExampleSet perLabelSets = null;
//        //Attribute label = null;
//        ExampleSet newExampleSet = null;
//        ExampleSet randomExampleSet = null;
//        List<String[]> valuePairs = null;
//        //int resultSize = 0;
//        Double setPart = 0.0;
//        String labelValue = "";
//        List<ExampleSet> newExampleSetList = new LinkedList<ExampleSet>();
//        Example nextExample;
//        
//        // get label attribute
//        label = originalSet.getAttributes().getLabel();
//        if (label != null) {
//        	if (label.isNominal()) {
//        		perLabelSets = SplittedExampleSet.splitByAttribute(originalSet, label);
//            } 
//            else {
//                throw new UserError(this, 105);
//            }
//        } 
//        else {
//            throw new UserError(this, resultSize);
//        }
//            
//        valuePairs = getParameterList(PARAMETER_SAMPLE_SIZE_PER_CLASS_LIST);
//
//        // try to perform data balancing for every class
//        for(int i = 0; i < perLabelSets.getNumberOfSubsets(); i++){
//        	
//        	perLabelSets.clearSelection();
//        	perLabelSets.selectAdditionalSubset(i);
//        		
//        	Double parameter = -1.0;
//        		
//        	nextExample = perLabelSets.iterator().next();
//        	labelValue = nextExample.getValueAsString(label);
//        		
//        	// check if there is any pair for current class
//        	for(String[] pair : valuePairs){
//        		if(labelValue.equals(pair[0])) {
//        			parameter = Double.valueOf(pair[1]);
//        			break;
//        		}
//        	}
//        		
//        	setPart = 0.0;
//        	
//        	// removing examples from subset
//        	if(parameter < 1.0 && parameter >= 0.0) {
//        		setPart = perLabelSets.size() * (1.0 - parameter);
//        		randomExampleSet = generateRandomExampleSetWithRemovedExamples(
//        				createExampleSetFromIndiceList(originalSet, createParentIndices(perLabelSets)), 
//        				setPart.intValue());
//        		newExampleSetList.add(randomExampleSet);
//        	}
//        	// adding examples to subset
//        	else if(parameter > 1.0) {
//        		setPart = perLabelSets.size() * (parameter - 1.0);
//        		randomExampleSet = generateRandomExampleSet(createExampleSetFromIndiceList(originalSet, 
//        				createParentIndices(perLabelSets)), setPart.intValue());
//        		newExampleSetList.add(randomExampleSet);
//        	}
//        	// copy current subset
//        	else {
//        		randomExampleSet = createExampleSetFromIndiceList(originalSet, createParentIndices(perLabelSets));
//        		newExampleSetList.add(randomExampleSet);
//        	}
//        }
//
//        // merge all created subsets
//        newExampleSet = mergeOperator.merge(newExampleSetList);
//        
//        return newExampleSet;
        
        
	}
	
	/**
	 * Creates new ExampleSet object. It contains examples from example set from parameter exampleSet and 
	 * random chosen rows from exampleSet. NumberOfIndices tells how many new examples should be selected.
	 * @param exampleSet - original example set
	 * @param numberOfIndices - number of new examples
	 * @return ExampleSet object with examples from exampleSet and new randomly selected from exampleSet
	 */
	private ExampleSet generateRandomExampleSet(ExampleSet exampleSet, Integer numberOfIndices){
		Random randomGenerator = new Random();
		ExampleTable exampleTable = exampleSet.getExampleTable();
		MemoryExampleTable table = MemoryExampleTable.createCompleteCopy(exampleTable);	
		
		for(int i = 0; i < numberOfIndices; i++){
			table.addDataRow(exampleTable.getDataRow(randomGenerator.nextInt(exampleTable.size())));
		}	
		
        return table.createExampleSet();
	}
	
	/**
	 * Creates new ExampleSet object. It is based on example set from parameter exampleSet but with randomly
	 * removed examples. NumberOfIndices tells how many examples should be removed.
	 * @param exampleSet - original example set
	 * @param numberOfIndices - number of examples to remove
	 * @return ExampleSet object with remaining examples from exampleSet
	 */
	private ExampleSet generateRandomExampleSetWithRemovedExamples(ExampleSet exampleSet, Integer numberOfIndices){
		Random randomGenerator = new Random();
		ExampleTable exampleTable = exampleSet.getExampleTable();
		MemoryExampleTable table = MemoryExampleTable.createCompleteCopy(exampleTable);
				
		if(numberOfIndices > exampleTable.size())
			numberOfIndices = exampleTable.size();
		
		for(int i = 0; i < numberOfIndices; i++){
			 table.removeDataRow(randomGenerator.nextInt(table.size()));
		}
		
        return table.createExampleSet();
	}
	
	/**
	 * Calculates parent indices for current subset.
	 * @param set - current subset with indices to calculate
	 * @return calculated parent indices list
	 */
	private List<Integer> createParentIndices(SplittedExampleSet set){
		List<Integer> indicesList = new LinkedList<Integer>();
		for(int i = 0; i < set.size(); i++){
			indicesList.add(set.getActualParentIndex(i));
		}
		
		return indicesList;
	}
	
	/**
	 * Creates new ExampleSet object as a subset of example set from parameter set. 
	 * Indices list has indices for elements in example set from parameter set. 
	 * @param set - actual set
	 * @param indicesList - list of indices for example set
	 * @return ExampleSet object which is a subset of parameter set 
	 */
	private ExampleSet createExampleSetFromIndiceList(ExampleSet set, List<Integer> indicesList){
		ExampleTable setTable = set.getExampleTable();
		Attribute[] attributes = setTable.getAttributes();
		MemoryExampleTable table = new MemoryExampleTable(attributes);
		
		for(int i = 0; i < indicesList.size(); i++){
			table.addDataRow(setTable.getDataRow(indicesList.get(i)));
		}
		
		return table.createExampleSet();
	}
	
	@Override
	/**
	 * Creates parameters for operator.
	 */
	public List<ParameterType> getParameterTypes() {
	        List<ParameterType> types = super.getParameterTypes();
	        // TODO: Try to find optimal defaults resulting from SMOTE paper
	        ParameterType type = new ParameterTypeInt(PARAMETER_SMOTE_N, "Amount of Smote N%. The amount of SMOTE is assumed to be integral multiplies of 100.", 0, Integer.MAX_VALUE, 300);
	        ParameterType type2 = new ParameterTypeInt(PARAMETER_NEAREST_NEIGHBORS, "Numbers of nearest neighbors taken into consideration when constructing synthetic examples.", 0, Integer.MAX_VALUE, 3);
	        ParameterType type3 = new ParameterTypeString(PARAMETER_MINORITY_CLASS_NAME, "Minority class name", false);
	        type.setExpert(false);
	        type2.setExpert(false);
	        type3.setExpert(false);
	        types.add(type);
	        types.add(type2);
	        types.add(type3);
	        
	        return types;
	}
	
    @Override
    /**
     * Operations to process.
     */
    public final void doWork() throws OperatorException {
        ExampleSet inputExampleSet = exampleSetInput.getData(ExampleSet.class);
        ExampleSet result = apply((ExampleSet) inputExampleSet.clone());
        originalOutput.deliver(inputExampleSet);
        exampleSetOutput.deliver(result);       
    }
}