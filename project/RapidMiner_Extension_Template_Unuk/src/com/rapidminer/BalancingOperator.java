package com.rapidminer;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.example.Attribute;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.operator.preprocessing.join.ExampleSetMerge;
import com.rapidminer.tools.OperatorService;

import java.util.Random;
import java.util.List;
import java.util.LinkedList;

public class BalancingOperator extends Operator {

	public static final String PARAMETER_SAMPLE_SIZE_PER_CLASS_LIST = "sample_size_per_class";
	private final InputPort exampleSetInput = getInputPorts().createPort(
			"example set input");
	private final OutputPort exampleSetOutput = getOutputPorts().createPort(
			"example set output");
	private final OutputPort originalOutput = getOutputPorts().createPort(
			"original");

	public BalancingOperator(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new SimplePrecondition(exampleSetInput,
				getRequiredMetaData()));
		getTransformer().addPassThroughRule(exampleSetInput, originalOutput);
		getTransformer().addRule(
				new PassThroughRule(exampleSetInput, exampleSetOutput, false) {

					@Override
					public MetaData modifyMetaData(MetaData metaData) {
						if (metaData instanceof ExampleSetMetaData) {
							ExampleSetMetaData exampleSetMetaData = ((ExampleSetMetaData) metaData);

							exampleSetMetaData
									.setNumberOfExamples(exampleSetMetaData
											.getNumberOfExamples());
						} else {
							return metaData;
						}
						return metaData;
					}

				});
	}

	/**
	 * Creates required meta data object.
	 * 
	 * @return ExampleSet meta data
	 */
	protected ExampleSetMetaData getRequiredMetaData() {
		return new ExampleSetMetaData();
	}

	/**
	 * Performs actual data balancing.
	 * 
	 * @param originalSet
	 *            - original example set from operator input
	 * @return balanced example set
	 * @throws OperatorException
	 */
	private ExampleSet apply(ExampleSet originalSet) throws OperatorException {

		ExampleSetMerge mergeOperator;
		try {
			mergeOperator = OperatorService
					.createOperator(ExampleSetMerge.class);
		} catch (OperatorCreationException e) {
			e.printStackTrace();
			return originalSet;
		}

		SplittedExampleSet perLabelSets = null;
		Attribute label = null;
		ExampleSet newExampleSet = null;
		ExampleSet randomExampleSet = null;
		List<String[]> valuePairs = null;
		int resultSize = 0;
		Double setPart = 0.0;
		String labelValue = "";
		List<ExampleSet> newExampleSetList = new LinkedList<ExampleSet>();
		Example nextExample;

		// get label attribute
		label = originalSet.getAttributes().getLabel();
		if (label != null) {
			if (label.isNominal()) {
				perLabelSets = SplittedExampleSet.splitByAttribute(originalSet,
						label);
			} else {
				throw new UserError(this, 105);
			}
		} else {
			throw new UserError(this, resultSize);
		}

		valuePairs = getParameterList(PARAMETER_SAMPLE_SIZE_PER_CLASS_LIST);

		// try to perform data balancing for every class
		for (int i = 0; i < perLabelSets.getNumberOfSubsets(); i++) {

			perLabelSets.clearSelection();
			perLabelSets.selectAdditionalSubset(i);

			Double parameter = -1.0;

			nextExample = perLabelSets.iterator().next();
			labelValue = nextExample.getValueAsString(label);

			// check if there is any pair for current class
			for (String[] pair : valuePairs) {
				if (labelValue.equals(pair[0])) {
					parameter = Double.valueOf(pair[1]);
					break;
				}
			}

			setPart = 0.0;

			// removing examples from subset
			if (parameter < 1.0 && parameter >= 0.0) {
				setPart = perLabelSets.size() * (1.0 - parameter);
				randomExampleSet = generateRandomExampleSetWithRemovedExamples(
						createExampleSetFromIndiceList(originalSet,
								createParentIndices(perLabelSets)),
						setPart.intValue());
				newExampleSetList.add(randomExampleSet);
			}
			// adding examples to subset
			else if (parameter > 1.0) {
				setPart = perLabelSets.size() * (parameter - 1.0);
				randomExampleSet = generateRandomExampleSet(
						createExampleSetFromIndiceList(originalSet,
								createParentIndices(perLabelSets)),
						setPart.intValue());
				newExampleSetList.add(randomExampleSet);
			}
			// copy current subset
			else {
				randomExampleSet = createExampleSetFromIndiceList(originalSet,
						createParentIndices(perLabelSets));
				newExampleSetList.add(randomExampleSet);
			}
		}

		// merge all created subsets
		newExampleSet = mergeOperator.merge(newExampleSetList);

		return newExampleSet;
	}

	/**
	 * Creates new ExampleSet object. It contains examples from example set from
	 * parameter exampleSet and random chosen rows from exampleSet.
	 * NumberOfIndices tells how many new examples should be selected.
	 * 
	 * @param exampleSet
	 *            - original example set
	 * @param numberOfIndices
	 *            - number of new examples
	 * @return ExampleSet object with examples from exampleSet and new randomly
	 *         selected from exampleSet
	 */
	private ExampleSet generateRandomExampleSet(ExampleSet exampleSet,
			Integer numberOfIndices) {
		Random randomGenerator = new Random();
		ExampleTable exampleTable = exampleSet.getExampleTable();
		MemoryExampleTable table = MemoryExampleTable
				.createCompleteCopy(exampleTable);

		for (int i = 0; i < numberOfIndices; i++) {
			table.addDataRow(exampleTable.getDataRow(randomGenerator
					.nextInt(exampleTable.size())));
		}

		return table.createExampleSet();
	}

	/**
	 * Creates new ExampleSet object. It is based on example set from parameter
	 * exampleSet but with randomly removed examples. NumberOfIndices tells how
	 * many examples should be removed.
	 * 
	 * @param exampleSet
	 *            - original example set
	 * @param numberOfIndices
	 *            - number of examples to remove
	 * @return ExampleSet object with remaining examples from exampleSet
	 */
	private ExampleSet generateRandomExampleSetWithRemovedExamples(
			ExampleSet exampleSet, Integer numberOfIndices) {
		Random randomGenerator = new Random();
		ExampleTable exampleTable = exampleSet.getExampleTable();
		MemoryExampleTable table = MemoryExampleTable
				.createCompleteCopy(exampleTable);

		if (numberOfIndices > exampleTable.size())
			numberOfIndices = exampleTable.size();

		for (int i = 0; i < numberOfIndices; i++) {
			table.removeDataRow(randomGenerator.nextInt(table.size()));
		}

		return table.createExampleSet();
	}

	/**
	 * Calculates parent indices for current subset.
	 * 
	 * @param set
	 *            - current subset with indices to calculate
	 * @return calculated parent indices list
	 */
	private List<Integer> createParentIndices(SplittedExampleSet set) {
		List<Integer> indicesList = new LinkedList<Integer>();
		for (int i = 0; i < set.size(); i++) {
			indicesList.add(set.getActualParentIndex(i));
		}

		return indicesList;
	}

	/**
	 * Creates new ExampleSet object as a subset of example set from parameter
	 * set. Indices list has indices for elements in example set from parameter
	 * set.
	 * 
	 * @param set
	 *            - actual set
	 * @param indicesList
	 *            - list of indices for example set
	 * @return ExampleSet object which is a subset of parameter set
	 */
	private ExampleSet createExampleSetFromIndiceList(ExampleSet set,
			List<Integer> indicesList) {
		ExampleTable setTable = set.getExampleTable();
		Attribute[] attributes = setTable.getAttributes();
		MemoryExampleTable table = new MemoryExampleTable(attributes);

		for (int i = 0; i < indicesList.size(); i++) {
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
		ParameterType type = new ParameterTypeList(
				PARAMETER_SAMPLE_SIZE_PER_CLASS_LIST,
				"The fraction per class.", new ParameterTypeString("class",
						"The class name this sample size applies to."),
				new ParameterTypeDouble("ratio",
						"The fractions of examples of this class.", 0,
						Double.MAX_VALUE));
		type.setExpert(false);
		types.add(type);

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