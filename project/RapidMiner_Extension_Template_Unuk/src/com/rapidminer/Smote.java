package com.rapidminer;

import com.rapidminer.operator.Operator;
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
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.example.Attribute;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.tools.math.container.GeometricDataCollection;
import com.rapidminer.tools.math.container.LinearList;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.DistanceMeasureHelper;
import com.rapidminer.tools.math.similarity.DistanceMeasures;

import java.util.Collection;
import java.util.Iterator;
import java.util.Random;
import java.util.List;


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
     * Performs teh SMOTEing.
     * @param originalSet - original example set from operator input
     * @return SMOTEd example set
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

        DistanceMeasure measure = measureHelper.getInitializedMeasure(originalSet);
    	
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
        
        for (Example example: minoritySet) {
        	int index = 0;
        	boolean debug = false;
        	double[] values = new double[valuesSize];
        	for (Attribute attribute: attributes) {
				values[index] = example.getValue(attribute);
				index++;
			}
        	if (debug) System.out.println("Sample");
    		for (int i=0; i<4; i++) {
    			if (debug) System.out.println(values[i]);
    		}
        	
        	// calculate k nearest neighbors
        	Collection<double[]> neighborValues = samples.getNearestValues(k, values);
        	// I hate Java
        	Iterator<double[]> it = neighborValues.iterator();
        	double[][] neighborArray = new double[neighborValues.size()][];

        	
        	index = 0;

    		if (debug) System.out.println("All neighbors");
        	while (it.hasNext()) {
        		neighborArray[index] = it.next();
        		if (debug) System.out.println(index);
        		for (int i=0; i<4; i++) {
        			if (debug) System.out.println(neighborArray[index][i]);
        		}
        		index++;
        	}
        	Random generator = new Random();
        	
        	// generate synthetic examples
        	for (int j=0; j<n; j++) {
        		DataRow sample = example.getDataRow();
        		double[] newDataRowData = new double[smotedTable.getNumberOfAttributes()];
                for (int a = 0; a < smotedTable.getNumberOfAttributes(); a++) {
                    Attribute attribute = smotedTable.getAttribute(a);
                    if (attribute != null) {
                        newDataRowData[a] = sample.get(attribute);
                    } else {
                        newDataRowData[a] = Double.NaN;
                    }
                }
                smotedTable.addDataRow(new DoubleArrayDataRow(newDataRowData));
        		DataRow synthetic = smotedTable.getDataRow(smotedTable.size() - 1);
        		// get random neighbor
        		double[] randomNeighborValues = neighborArray[generator.nextInt(neighborValues.size() - 1)];
        		if (debug) System.out.println("Chosen");
        		for (int i=0; i<4; i++) {
        			if (debug) System.out.println(randomNeighborValues[i]);
        		}
        		// for each continuous attribute
        		index = 0;
        		for (Attribute attribute: attributes) {
        			// calculate distance
    				double distance = Math.abs(randomNeighborValues[index] - values[index]);
    				// generate random gap
    				double gap = generator.nextDouble();
    				
    				// attr = sample attr + distance * gap
    				double newValue = 0.0;
    				if (randomNeighborValues[index] > values[index]) {
    					newValue = values[index] + distance * gap;
    				}
    				else {
    					newValue = randomNeighborValues[index] + distance * gap;
    				}
    				
    				if (debug) System.out.print("Sample: ");
        			if (debug) System.out.println(values[index]);
        			if (debug) System.out.print("Neighbor: ");
        			if (debug) System.out.println(randomNeighborValues[index]);
        			if (debug) System.out.print("New: ");
        			if (debug) System.out.println(newValue);

    				synthetic.set(attribute, newValue);
    				index++;
    			}
        	}
        	debug = false;

			checkForStop();
        }
        
        return smotedTable.createExampleSet();
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
	        types.addAll(DistanceMeasures.getParameterTypes(this));
	        
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