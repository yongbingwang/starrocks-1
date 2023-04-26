// Copyright 2021-present StarRocks, Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.starrocks.sql.optimizer.operator.logical;

import com.google.common.collect.Lists;
import com.starrocks.catalog.TableFunction;
import com.starrocks.common.Pair;
import com.starrocks.sql.optimizer.ExpressionContext;
import com.starrocks.sql.optimizer.OptExpression;
import com.starrocks.sql.optimizer.OptExpressionVisitor;
import com.starrocks.sql.optimizer.RowOutputInfo;
import com.starrocks.sql.optimizer.base.ColumnRefSet;
import com.starrocks.sql.optimizer.operator.ColumnOutputInfo;
import com.starrocks.sql.optimizer.operator.OperatorType;
import com.starrocks.sql.optimizer.operator.OperatorVisitor;
import com.starrocks.sql.optimizer.operator.scalar.ColumnRefOperator;
import com.starrocks.sql.optimizer.operator.scalar.ScalarOperator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LogicalTableFunctionOperator extends LogicalOperator {
    private final TableFunction fn;

    // Table function own output cols. It's used to validate plan.
    private final List<ColumnRefOperator> fnResultColRefs;

    // External column ref of the join logic generated by the table function
    private final List<ColumnRefOperator> outerColRefs;

    // Table function input parameters.
    private final List<Pair<ColumnRefOperator, ScalarOperator>> fnParamColumnProject;

    public LogicalTableFunctionOperator(List<ColumnRefOperator> fnResultColRefs, TableFunction fn,
                                        List<Pair<ColumnRefOperator, ScalarOperator>> fnParamColumnProject,
                                        List<ColumnRefOperator> outerColRefs) {
        super(OperatorType.LOGICAL_TABLE_FUNCTION);
        this.fnResultColRefs = fnResultColRefs;
        this.fn = fn;
        this.fnParamColumnProject = fnParamColumnProject;
        this.outerColRefs = outerColRefs;
    }

    public LogicalTableFunctionOperator(List<ColumnRefOperator> fnResultColRefs, TableFunction fn,
                                        List<Pair<ColumnRefOperator, ScalarOperator>> fnParamColumnProject) {
        this(fnResultColRefs, fn, fnParamColumnProject, Lists.newArrayList());
    }

    private LogicalTableFunctionOperator(Builder builder) {
        super(OperatorType.LOGICAL_TABLE_FUNCTION, builder.getLimit(), builder.getPredicate(), builder.getProjection());
        this.fnResultColRefs = builder.fnResultColRefs;
        this.fn = builder.fn;
        this.fnParamColumnProject = builder.fnParamColumnProject;
        this.outerColRefs = builder.outerColRefs;
    }

    public List<ColumnRefOperator> getFnResultColRefs() {
        return fnResultColRefs;
    }

    public TableFunction getFn() {
        return fn;
    }

    public List<Pair<ColumnRefOperator, ScalarOperator>> getFnParamColumnProject() {
        return fnParamColumnProject;
    }

    public List<ColumnRefOperator> getOuterColRefs() {
        return outerColRefs;
    }

    // Table function node combines its child output cols and its own output cols
    public List<ColumnRefOperator> getOutputColRefs() {
        List<ColumnRefOperator> outputCols = Lists.newArrayList();
        outputCols.addAll(outerColRefs);
        outputCols.addAll(fnResultColRefs);
        return outputCols;
    }


    @Override
    public ColumnRefSet getOutputColumns(ExpressionContext expressionContext) {
        if (projection != null) {
            return new ColumnRefSet(new ArrayList<>(projection.getColumnRefMap().keySet()));
        } else {
            ColumnRefSet outputColumns = new ColumnRefSet(outerColRefs);
            outputColumns.union(new ColumnRefSet(fnResultColRefs));
            return outputColumns;
        }
    }

    @Override
    public RowOutputInfo deriveRowOutputInfo(List<OptExpression> inputs) {
        List<ColumnOutputInfo> outputInfoList = Lists.newArrayList();
        for (ColumnRefOperator col : fnResultColRefs) {
            outputInfoList.add(new ColumnOutputInfo(col, col));
        }

        for (ColumnRefOperator col : outerColRefs) {
            outputInfoList.add(new ColumnOutputInfo(col, col));
        }
        return new RowOutputInfo(outputInfoList, outerColRefs);
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C context) {
        return visitor.visitLogicalTableFunction(this, context);
    }

    @Override
    public <R, C> R accept(OptExpressionVisitor<R, C> visitor, OptExpression optExpression, C context) {
        return visitor.visitLogicalTableFunction(optExpression, context);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!super.equals(o)) {
            return false;
        }

        LogicalTableFunctionOperator that = (LogicalTableFunctionOperator) o;
        return Objects.equals(fn, that.fn) && Objects.equals(fnResultColRefs, that.fnResultColRefs)
                && Objects.equals(outerColRefs, that.outerColRefs)
                && Objects.equals(fnParamColumnProject, that.fnParamColumnProject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fn, fnResultColRefs);
    }

    public static class Builder
            extends LogicalOperator.Builder<LogicalTableFunctionOperator, LogicalTableFunctionOperator.Builder> {
        private TableFunction fn;
        private List<ColumnRefOperator> fnResultColRefs;
        private List<ColumnRefOperator> outerColRefs;
        private List<Pair<ColumnRefOperator, ScalarOperator>> fnParamColumnProject;

        @Override
        public LogicalTableFunctionOperator build() {
            return new LogicalTableFunctionOperator(this);
        }

        public LogicalTableFunctionOperator.Builder setOuterColRefs(List<ColumnRefOperator> outerColRefs) {
            this.outerColRefs = outerColRefs;
            return this;
        }

        @Override
        public LogicalTableFunctionOperator.Builder withOperator(LogicalTableFunctionOperator tableFunctionOperator) {
            super.withOperator(tableFunctionOperator);
            this.fnResultColRefs = tableFunctionOperator.fnResultColRefs;
            this.fn = tableFunctionOperator.fn;
            this.fnParamColumnProject = tableFunctionOperator.fnParamColumnProject;
            this.outerColRefs = tableFunctionOperator.outerColRefs;
            return this;
        }
    }
}