/**
 *    Copyright 2016-2017 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.dynamic.sql.where.render;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlCriterion;

public class CriterionRenderer<T> {
    private StringBuilder buffer = new StringBuilder();
    private Map<String, Object> parameters = new HashMap<>();
    private SqlCriterion<T> criterion;
    private AtomicInteger sequence;
    private Function<SqlColumn<?>, String> nameFunction;
    
    protected CriterionRenderer(SqlCriterion<T> criterion, AtomicInteger sequence, Function<SqlColumn<?>, String> nameFunction) {
        this.criterion = criterion;
        this.sequence = sequence;
        this.nameFunction = nameFunction;
    }
    
    public RenderedCriterion render() {
        buffer.append(' ');
        
        renderConnector();

        if (criterion.hasSubCriteria()) {
            buffer.append('(');
            renderCriteria();
            buffer.append(')');
        } else {
            renderCriteria();
        }
        
        return RenderedCriterion.of(buffer.toString(), parameters);
    }
    
    private void renderConnector() {
        criterion.connector().ifPresent(c -> {
            buffer.append(c);
            buffer.append(' ');
        });
    }
    
    private void renderCriteria() {
        renderCondition();
        criterion.subCriteria().forEach(this::handleSubCriterion);
    }

    private void renderCondition() {
        ConditionRenderer<T> visitor = ConditionRenderer.of(sequence, criterion.column(), nameFunction);
        criterion.condition().accept(visitor);
        buffer.append(visitor.fragment());
        parameters.putAll(visitor.parameters());
    }

    private void handleSubCriterion(SqlCriterion<?> subCriterion) {
        RenderedCriterion rc = new CriterionRenderer<>(subCriterion, sequence, nameFunction).render();
        buffer.append(rc.whereClauseFragment());
        parameters.putAll(rc.fragmentParameters());
    }
    
    public static <T> CriterionRenderer<T> of(SqlCriterion<T> criterion, AtomicInteger sequence, Function<SqlColumn<?>, String> nameFunction) {
        return new CriterionRenderer<>(criterion, sequence, nameFunction);
    }
}