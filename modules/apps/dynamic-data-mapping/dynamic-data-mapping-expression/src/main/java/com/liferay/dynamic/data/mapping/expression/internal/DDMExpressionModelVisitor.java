/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.dynamic.data.mapping.expression.internal;

import com.liferay.dynamic.data.mapping.expression.internal.parser.DDMExpressionBaseVisitor;
import com.liferay.dynamic.data.mapping.expression.internal.parser.DDMExpressionParser;
import com.liferay.dynamic.data.mapping.expression.model.AndExpression;
import com.liferay.dynamic.data.mapping.expression.model.ArithmeticExpression;
import com.liferay.dynamic.data.mapping.expression.model.ArrayExpression;
import com.liferay.dynamic.data.mapping.expression.model.ComparisonExpression;
import com.liferay.dynamic.data.mapping.expression.model.Expression;
import com.liferay.dynamic.data.mapping.expression.model.FloatingPointLiteral;
import com.liferay.dynamic.data.mapping.expression.model.FunctionCallExpression;
import com.liferay.dynamic.data.mapping.expression.model.IntegerLiteral;
import com.liferay.dynamic.data.mapping.expression.model.MinusExpression;
import com.liferay.dynamic.data.mapping.expression.model.NotExpression;
import com.liferay.dynamic.data.mapping.expression.model.OrExpression;
import com.liferay.dynamic.data.mapping.expression.model.Parenthesis;
import com.liferay.dynamic.data.mapping.expression.model.StringLiteral;
import com.liferay.dynamic.data.mapping.expression.model.Term;
import com.liferay.portal.kernel.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * @author Marcellus Tavares
 */
public class DDMExpressionModelVisitor
	extends DDMExpressionBaseVisitor<Expression> {

	@Override
	public Expression visitAdditionExpression(
		DDMExpressionParser.AdditionExpressionContext context) {

		Expression l = visitChild(context, 0);
		Expression r = visitChild(context, 2);

		return new ArithmeticExpression("+", l, r);
	}

	@Override
	public Expression visitAndExpression(
		DDMExpressionParser.AndExpressionContext context) {

		Expression l = visitChild(context, 0);
		Expression r = visitChild(context, 2);

		return new AndExpression(l, r);
	}

	@Override
	public Expression visitArray(DDMExpressionParser.ArrayContext context) {
		return new ArrayExpression(context.getText());
	}

	@Override
	public Expression visitBooleanParenthesis(
		DDMExpressionParser.BooleanParenthesisContext context) {

		return visitChild(context, 1);
	}

	@Override
	public Expression visitDivisionExpression(
		DDMExpressionParser.DivisionExpressionContext context) {

		Expression l = visitChild(context, 0);
		Expression r = visitChild(context, 2);

		return new ArithmeticExpression("/", l, r);
	}

	@Override
	public Expression visitEqualsExpression(
		DDMExpressionParser.EqualsExpressionContext context) {

		Expression l = visitChild(context, 0);
		Expression r = visitChild(context, 2);

		return new ComparisonExpression("=", l, r);
	}

	@Override
	public Expression visitExpression(
		DDMExpressionParser.ExpressionContext context) {

		DDMExpressionParser.LogicalOrExpressionContext
			logicalOrExpressionContext = context.logicalOrExpression();

		return logicalOrExpressionContext.accept(this);
	}

	@Override
	public Expression visitFloatingPointLiteral(
		DDMExpressionParser.FloatingPointLiteralContext context) {

		return new FloatingPointLiteral(context.getText());
	}

	@Override
	public Expression visitFunctionCallExpression(
		DDMExpressionParser.FunctionCallExpressionContext context) {

		String functionName = getFunctionName(context.functionName);

		List<Expression> parameters = getFunctionParameters(
			context.functionParameters());

		return new FunctionCallExpression(functionName, parameters);
	}

	@Override
	public Expression visitGreaterThanExpression(
		DDMExpressionParser.GreaterThanExpressionContext context) {

		Expression l = visitChild(context, 0);
		Expression r = visitChild(context, 2);

		return new ComparisonExpression(">", l, r);
	}

	@Override
	public Expression visitGreaterThanOrEqualsExpression(
		DDMExpressionParser.GreaterThanOrEqualsExpressionContext context) {

		Expression l = visitChild(context, 0);
		Expression r = visitChild(context, 2);

		return new ComparisonExpression(">=", l, r);
	}

	@Override
	public Expression visitIntegerLiteral(
		DDMExpressionParser.IntegerLiteralContext context) {

		return new IntegerLiteral(context.getText());
	}

	@Override
	public Expression visitLessThanExpression(
		DDMExpressionParser.LessThanExpressionContext context) {

		Expression l = visitChild(context, 0);
		Expression r = visitChild(context, 2);

		return new ComparisonExpression("<", l, r);
	}

	@Override
	public Expression visitLessThanOrEqualsExpression(
		DDMExpressionParser.LessThanOrEqualsExpressionContext context) {

		Expression l = visitChild(context, 0);
		Expression r = visitChild(context, 2);

		return new ComparisonExpression("<=", l, r);
	}

	@Override
	public Expression visitLogicalConstant(
		DDMExpressionParser.LogicalConstantContext context) {

		return new Term(context.getText());
	}

	@Override
	public Expression visitLogicalVariable(
		DDMExpressionParser.LogicalVariableContext context) {

		return new Term(context.getText());
	}

	@Override
	public Expression visitMinusExpression(
		DDMExpressionParser.MinusExpressionContext context) {

		Expression expression = visitChild(context, 1);

		return new MinusExpression(expression);
	}

	@Override
	public Expression visitMultiplicationExpression(
		DDMExpressionParser.MultiplicationExpressionContext context) {

		Expression l = visitChild(context, 0);
		Expression r = visitChild(context, 2);

		return new ArithmeticExpression("*", l, r);
	}

	@Override
	public Expression visitNotEqualsExpression(
		DDMExpressionParser.NotEqualsExpressionContext context) {

		Expression l = visitChild(context, 0);
		Expression r = visitChild(context, 2);

		return new ComparisonExpression("!=", l, r);
	}

	@Override
	public Expression visitNotExpression(
		DDMExpressionParser.NotExpressionContext context) {

		Expression expression = visitChild(context, 1);

		if (expression instanceof Parenthesis) {
			Parenthesis parenthesis = (Parenthesis)expression;

			expression = parenthesis.getOperandExpression();
		}

		return new NotExpression(expression);
	}

	@Override
	public Expression visitNumericParenthesis(
		DDMExpressionParser.NumericParenthesisContext context) {

		return new Parenthesis(visitChild(context, 1));
	}

	@Override
	public Expression visitNumericVariable(
		DDMExpressionParser.NumericVariableContext context) {

		return new Term(context.getText());
	}

	@Override
	public Expression visitOrExpression(
		DDMExpressionParser.OrExpressionContext context) {

		Expression l = visitChild(context, 0);
		Expression r = visitChild(context, 2);

		return new OrExpression(l, r);
	}

	@Override
	public Expression visitStringLiteral(
		DDMExpressionParser.StringLiteralContext context) {

		return new StringLiteral(StringUtil.unquote(context.getText()));
	}

	@Override
	public Expression visitSubtractionExpression(
		DDMExpressionParser.SubtractionExpressionContext context) {

		Expression l = visitChild(context, 0);
		Expression r = visitChild(context, 2);

		return new ArithmeticExpression("-", l, r);
	}

	protected String getFunctionName(Token functionNameToken) {
		return functionNameToken.getText();
	}

	protected List<Expression> getFunctionParameters(
		DDMExpressionParser.FunctionParametersContext context) {

		if (context == null) {
			return Collections.emptyList();
		}

		List<Expression> parameters = new ArrayList<>();

		for (int i = 0; i < context.getChildCount(); i += 2) {
			Expression parameter = visitChild(context, i);

			parameters.add(parameter);
		}

		return parameters;
	}

	protected <T> T visitChild(
		ParserRuleContext parserRuleContext, int childIndex) {

		ParseTree parseTree = parserRuleContext.getChild(childIndex);

		return (T)parseTree.accept(this);
	}

}