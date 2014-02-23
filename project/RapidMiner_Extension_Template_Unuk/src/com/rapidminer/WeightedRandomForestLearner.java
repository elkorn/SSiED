package com.rapidminer;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.learner.tree.RandomForestLearner;
import com.rapidminer.operator.learner.tree.RandomForestModel;
import com.rapidminer.parameter.ParameterType;

public class WeightedRandomForestLearner extends RandomForestLearner {
	public WeightedRandomForestLearner(OperatorDescription description) {
		super(description);
		this.setParameter(PARAMETER_CRITERION, "gini_index");
	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return RandomForestModel.class;
	}
	
	@Override
	public List<ParameterType> getParameterTypes() {
		LinkedList<ParameterType> types = new LinkedList<ParameterType>();
		types.addAll(super.getParameterTypes());
		for (ParameterType type : types) {
			if(type.getKey().equalsIgnoreCase(PARAMETER_CRITERION)) {
				types.remove(type);
				break;
			}
		}

		return types;
	}
}