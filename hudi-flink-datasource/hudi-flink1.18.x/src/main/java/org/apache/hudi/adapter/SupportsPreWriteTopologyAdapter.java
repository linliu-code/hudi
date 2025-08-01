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

package org.apache.hudi.adapter;

import org.apache.flink.streaming.api.datastream.DataStream;

/**
 * {@code SupportsPreWriteTopology} is introduced for Sink V2 since Flink 1.19,
 * We add the adapter here to just make the compilation successful for earlier
 * Flink versions (< 1.19).
 */
public interface SupportsPreWriteTopologyAdapter<InputT> {
  /**
   * Adds an arbitrary topology before the writer. The topology may be used to repartition the
   * data.
   *
   * @param inputDataStream the stream of input records.
   * @return the custom topology before {@code SinkWriter}.
   */
  DataStream<InputT> addPreWriteTopology(DataStream<InputT> inputDataStream);
}
