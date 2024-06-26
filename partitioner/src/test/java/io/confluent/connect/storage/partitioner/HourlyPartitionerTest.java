/*
 * Copyright 2018 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.connect.storage.partitioner;

import io.confluent.connect.storage.StorageSinkTestBase;
import io.confluent.connect.storage.common.StorageCommonConfig;
import org.apache.kafka.connect.sink.SinkRecord;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class HourlyPartitionerTest extends StorageSinkTestBase {
  protected static final DateTimeZone DATE_TIME_ZONE = DateTimeZone.forID("America/Los_Angeles");

  @Test
  public void testHourlyPartitioner() {
    Map<String, Object> config = new HashMap<>();
    config.put(StorageCommonConfig.DIRECTORY_DELIM_CONFIG, StorageCommonConfig.DIRECTORY_DELIM_DEFAULT);
    config.put(PartitionerConfig.TIMESTAMP_EXTRACTOR_CLASS_CONFIG, "Record");
    config.put(PartitionerConfig.LOCALE_CONFIG, Locale.US.toString());
    config.put(PartitionerConfig.TIMEZONE_CONFIG, DATE_TIME_ZONE.toString());

    HourlyPartitioner<String> partitioner = new HourlyPartitioner<>();
    partitioner.configure(config);

    String timeZoneString = (String) config.get(PartitionerConfig.TIMEZONE_CONFIG);
    int year = 2014;
    int month = 2;
    int day = 1;
    int hour = 3;
    long timestamp = new DateTime(year, month, day, hour, 0, 0, 0, DateTimeZone.forID(timeZoneString)).getMillis();
    SinkRecord sinkRecord = createSinkRecord(timestamp);
    String encodedPartition = partitioner.encodePartition(sinkRecord);

    Map<String, Object> m = new LinkedHashMap<>();
    m.put("year", year);
    m.put("month", String.format("%02d", month));
    m.put("day", String.format("%02d", day));
    m.put("hour", String.format("%02d", hour));
    assertThat(encodedPartition, is(generateEncodedPartitionFromMap(m)));
  }

  @Test
  public void testHourlyPartionerWithDaylightSavings() {
    Map<String, Object> props = new HashMap<>();
    props.put(PartitionerConfig.TIMEZONE_CONFIG, "America/Santiago");

    props.put(PartitionerConfig.LOCALE_CONFIG, "es_CL");
    props.put(StorageCommonConfig.DIRECTORY_DELIM_CONFIG, StorageCommonConfig.DIRECTORY_DELIM_DEFAULT);
    props.put(PartitionerConfig.TIMESTAMP_EXTRACTOR_CLASS_CONFIG, "Record");

    HourlyPartitioner<String> partitioner = new HourlyPartitioner<>();
    partitioner.configure(props);

    // Daylight savings
    SinkRecord sinkRecord = createSinkRecord(1715224653000L); // 2024/05/09 03:17:33 UTC
    String encodedPartition = partitioner.encodePartition(sinkRecord);
    assertEquals("year=2024/month=05/day=08/hour=23", encodedPartition); //UTC-04:00

    // No daylight savings
    sinkRecord = createSinkRecord(1706411853000L); // 2024/01/28 03:17:33 UTC
    encodedPartition = partitioner.encodePartition(sinkRecord);
    assertEquals("year=2024/month=01/day=28/hour=00", encodedPartition); //UTC-03:00
  }
}