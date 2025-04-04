/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hudi.testutils;

import org.apache.hudi.SparkAdapterSupport$;
import org.apache.hudi.common.config.HoodieStorageConfig;
import org.apache.hudi.common.model.HoodieKey;
import org.apache.hudi.common.model.HoodieRecord;
import org.apache.hudi.common.table.view.FileSystemViewStorageConfig;
import org.apache.hudi.common.testutils.HoodieTestDataGenerator;
import org.apache.hudi.config.HoodieCompactionConfig;
import org.apache.hudi.config.HoodieIndexConfig;
import org.apache.hudi.config.HoodieWriteConfig;
import org.apache.hudi.index.HoodieIndex;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder;
import org.apache.spark.sql.catalyst.expressions.GenericInternalRow;
import org.apache.spark.sql.catalyst.expressions.GenericRow;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.apache.hudi.common.testutils.FileSystemTestUtils.RANDOM;

/**
 * Dataset test utils.
 * Note: This util class can be only used within `hudi-spark<spark_version>` modules because it
 * relies on SparkAdapterSupport to get encoder for different versions of Spark. If used elsewhere this
 * class won't be initialized properly amd could cause ClassNotFoundException or NoClassDefFoundError
 */
public class SparkDatasetTestUtils {

  public static final String RECORD_KEY_FIELD_NAME = "record_key";
  public static final String PARTITION_PATH_FIELD_NAME = "partition_path";

  public static final StructType STRUCT_TYPE = new StructType(new StructField[] {
      new StructField(HoodieRecord.COMMIT_TIME_METADATA_FIELD, DataTypes.StringType, false, Metadata.empty()),
      new StructField(HoodieRecord.COMMIT_SEQNO_METADATA_FIELD, DataTypes.StringType, false, Metadata.empty()),
      new StructField(HoodieRecord.RECORD_KEY_METADATA_FIELD, DataTypes.StringType, false, Metadata.empty()),
      new StructField(HoodieRecord.PARTITION_PATH_METADATA_FIELD, DataTypes.StringType, false, Metadata.empty()),
      new StructField(HoodieRecord.FILENAME_METADATA_FIELD, DataTypes.StringType, false, Metadata.empty()),
      new StructField(RECORD_KEY_FIELD_NAME, DataTypes.StringType, false, Metadata.empty()),
      new StructField(PARTITION_PATH_FIELD_NAME, DataTypes.StringType, false, Metadata.empty()),
      new StructField("randomInt", DataTypes.IntegerType, false, Metadata.empty()),
      new StructField("randomLong", DataTypes.LongType, false, Metadata.empty())});

  public static final StructType ERROR_STRUCT_TYPE = new StructType(new StructField[] {
      new StructField(HoodieRecord.COMMIT_TIME_METADATA_FIELD, DataTypes.StringType, false, Metadata.empty()),
      new StructField(HoodieRecord.COMMIT_SEQNO_METADATA_FIELD, DataTypes.LongType, false, Metadata.empty()),
      new StructField(HoodieRecord.RECORD_KEY_METADATA_FIELD, DataTypes.StringType, false, Metadata.empty()),
      new StructField(HoodieRecord.PARTITION_PATH_METADATA_FIELD, DataTypes.StringType, false, Metadata.empty()),
      new StructField(HoodieRecord.FILENAME_METADATA_FIELD, DataTypes.StringType, false, Metadata.empty()),
      new StructField(RECORD_KEY_FIELD_NAME, DataTypes.StringType, false, Metadata.empty()),
      new StructField(PARTITION_PATH_FIELD_NAME, DataTypes.StringType, false, Metadata.empty()),
      new StructField("randomInt", DataTypes.IntegerType, false, Metadata.empty()),
      new StructField("randomStr", DataTypes.StringType, false, Metadata.empty())});

  public static final ExpressionEncoder ENCODER = getEncoder(STRUCT_TYPE);
  public static final ExpressionEncoder ERROR_ENCODER = getEncoder(ERROR_STRUCT_TYPE);

  /**
   * Generate Encode for the passed in {@link StructType}.
   *
   * @param schema instance of {@link StructType} for which encoder is requested.
   * @return the encoder thus generated.
   */
  private static ExpressionEncoder getEncoder(StructType schema) {
    return SparkAdapterSupport$.MODULE$.sparkAdapter().getCatalystExpressionUtils().getEncoder(schema);
  }

  /**
   * Generate random Rows.
   *
   * @param count total number of Rows to be generated.
   * @param partitionPath partition path to be set
   * @return the Dataset<Row>s thus generated.
   */
  public static Dataset<Row> getRandomRows(SQLContext sqlContext, int count, String partitionPath, boolean isError) {
    List<Row> records = new ArrayList<>();
    for (long recordNum = 0; recordNum < count; recordNum++) {
      records.add(getRandomValue(partitionPath, isError, ""));
    }
    return sqlContext.createDataFrame(records, isError ? ERROR_STRUCT_TYPE : STRUCT_TYPE);
  }

  public static Dataset<Row> getRandomRowsWithCommitTime(SQLContext sqlContext,
                                                         int count,
                                                         String partitionPath,
                                                         boolean isError,
                                                         String commitTime) {
    List<Row> records = new ArrayList<>();
    for (long recordNum = 0; recordNum < count; recordNum++) {
      records.add(getRandomValue(partitionPath, isError, commitTime));
    }
    return sqlContext.createDataFrame(records, isError ? ERROR_STRUCT_TYPE : STRUCT_TYPE);
  }

  public static Dataset<Row> getRandomRowsWithKeys(SQLContext sqlContext,
                                                   List<HoodieKey> keys,
                                                   boolean isError,
                                                   String commitTime) {
    List<Row> records = new ArrayList<>();
    for (HoodieKey key : keys) {
      records.add(getRandomValue(key, isError, commitTime));
    }
    return sqlContext.createDataFrame(records, isError ? ERROR_STRUCT_TYPE : STRUCT_TYPE);
  }

  /**
   * Generate random Row.
   *
   * @param partitionPath partition path to be set in the Row.
   * @return the Row thus generated.
   */
  public static Row getRandomValue(String partitionPath, boolean isError, String commitTime) {
    // order commit time, seq no, record key, partition path, file name
    String recordKey = UUID.randomUUID().toString();
    Object[] values = new Object[9];
    values[0] = commitTime; //commit time
    if (!isError) {
      values[1] = ""; // commit seq no
    } else {
      values[1] = RANDOM.nextLong();
    }
    values[2] = recordKey;
    values[3] = partitionPath;
    values[4] = ""; // filename
    values[5] = recordKey;
    values[6] = partitionPath;
    values[7] = RANDOM.nextInt();
    if (!isError) {
      values[8] = RANDOM.nextLong();
    } else {
      values[8] = UUID.randomUUID().toString();
    }
    return new GenericRow(values);
  }

  /**
   * Generate random Row with a given key.
   *
   * @param key a {@link HoodieKey} key.
   * @return the Row thus generated.
   */
  public static Row getRandomValue(HoodieKey key, boolean isError, String commitTime) {
    // order commit time, seq no, record key, partition path, file name
    Object[] values = new Object[9];
    values[0] = commitTime; //commit time
    if (!isError) {
      values[1] = ""; // commit seq no
    } else {
      values[1] = RANDOM.nextLong();
    }
    values[2] = key.getRecordKey();
    values[3] = key.getPartitionPath();
    values[4] = ""; // filename
    values[5] = key.getRecordKey();
    values[6] = key.getPartitionPath();
    values[7] = RANDOM.nextInt();
    if (!isError) {
      values[8] = RANDOM.nextLong();
    } else {
      values[8] = UUID.randomUUID().toString();
    }
    return new GenericRow(values);
  }

  /**
   * Convert Dataset<Row>s to List of {@link InternalRow}s.
   *
   * @param rows Dataset<Row>s to be converted
   * @return the List of {@link InternalRow}s thus converted.
   */
  public static List<InternalRow> toInternalRows(Dataset<Row> rows, ExpressionEncoder encoder) throws Exception {
    List<InternalRow> toReturn = new ArrayList<>();
    List<Row> rowList = rows.collectAsList();
    for (Row row : rowList) {
      toReturn.add(serializeRow(encoder, row).copy());
    }
    return toReturn;
  }

  public static InternalRow getInternalRowWithError(String partitionPath) {
    // order commit time, seq no, record key, partition path, file name
    String recordKey = UUID.randomUUID().toString();
    Object[] values = new Object[9];
    values[0] = "";
    values[1] = "";
    values[2] = recordKey;
    values[3] = partitionPath;
    values[4] = "";
    values[5] = recordKey;
    values[6] = partitionPath;
    values[7] = RANDOM.nextInt();
    values[8] = RANDOM.nextBoolean();
    return new GenericInternalRow(values);
  }

  public static HoodieWriteConfig.Builder getConfigBuilder(String basePath, int timelineServicePort) {
    return HoodieWriteConfig.newBuilder().withPath(basePath).withSchema(HoodieTestDataGenerator.TRIP_EXAMPLE_SCHEMA)
        .withPopulateMetaFields(true)
        .withParallelism(2, 2)
        .withDeleteParallelism(2)
        .withCompactionConfig(HoodieCompactionConfig.newBuilder().compactionSmallFileSize(1024 * 1024).build())
        .withStorageConfig(HoodieStorageConfig.newBuilder().hfileMaxFileSize(1024 * 1024).parquetMaxFileSize(1024 * 1024).build())
        .withFileSystemViewConfig(FileSystemViewStorageConfig.newBuilder()
            .withRemoteServerPort(timelineServicePort).build())
        .forTable("test-trip-table")
        .withIndexConfig(HoodieIndexConfig.newBuilder().withIndexType(HoodieIndex.IndexType.BLOOM).build())
        .withBulkInsertParallelism(2);
  }

  public static InternalRow serializeRow(ExpressionEncoder encoder, Row row)
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    Object serializer = encoder.createSerializer();
    Method applyMethod = serializer.getClass().getMethod("apply", Object.class);
    return (InternalRow) applyMethod.invoke(serializer, row);
  }
}
