package com.rapidminer;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.AttributeWeightedExampleSet;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.features.weighting.GiniWeighting;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.learner.tree.RandomForestLearner;
import com.rapidminer.operator.learner.tree.RandomForestModel;

import com.rapidminer.tools.OperatorService;

public class WeightedRandomForestLearner extends RandomForestLearner {
	public WeightedRandomForestLearner(OperatorDescription description) {
		super(description);
		super.setParameter(RandomForestLearner.PARAMETER_CRITERION,
				"gini_index");
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return RandomForestModel.class;
	}

	@Override
	public Model learn(ExampleSet exampleSet) throws OperatorException {
//		GiniWeighting giniWeighting = null;
//		try {
//			giniWeighting = OperatorService.createOperator(GiniWeighting.class);
			// Parameterize giniWeighting
//		} catch (OperatorCreationException e) {
//			throw new OperatorException(getName()
//					+ ": cannot construct random forest learner: "
//					+ e.getMessage());
//		}

//		AttributeWeightedExampleSet weightedExampleSet = new AttributeWeightedExampleSet(
//				exampleSet, giniWeighting.doWork(exampleSet));

		return super.learn(exampleSet);
	}
}
