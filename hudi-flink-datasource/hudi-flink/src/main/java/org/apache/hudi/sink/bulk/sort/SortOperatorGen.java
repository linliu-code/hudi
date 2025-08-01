/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hudi.sink.bulk.sort;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.table.api.TableConfig;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.planner.codegen.sort.SortCodeGenerator;
import org.apache.flink.table.planner.plan.nodes.exec.spec.SortSpec;
import org.apache.flink.table.types.logical.RowType;

import java.util.Arrays;

/**
 * Tools to generate the sort operator.
 */
public class SortOperatorGen {
  private final int[] sortIndices;
  private final RowType rowType;
  private final TableConfig tableConfig = TableConfig.getDefault();

  public SortOperatorGen(RowType rowType, String[] sortFields) {
    this.sortIndices = Arrays.stream(sortFields).mapToInt(rowType::getFieldIndex).toArray();
    this.rowType = rowType;
  }

  public OneInputStreamOperator<RowData, RowData> createSortOperator(Configuration conf) {
    SortCodeGenerator codeGen = createSortCodeGenerator();
    return new SortOperator(
        codeGen.generateNormalizedKeyComputer("SortComputer"),
        codeGen.generateRecordComparator("SortComparator"),
        conf);
  }

  public SortCodeGenerator createSortCodeGenerator() {
    SortSpec.SortSpecBuilder builder = SortSpec.builder();
    for (int sortIndex : sortIndices) {
      builder.addField(sortIndex, true, true);
    }
    return new SortCodeGenerator(tableConfig, Thread.currentThread().getContextClassLoader(), rowType, builder.build());
  }
}
