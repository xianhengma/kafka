/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kafka.consumer

import kafka.utils.{Pool, threadsafe, Logging}
import java.util.concurrent.TimeUnit
import kafka.metrics.KafkaMetricsGroup
import kafka.common.ClientIdAndTopic

@threadsafe
class ConsumerTopicMetrics(metricId: ClientIdAndTopic) extends KafkaMetricsGroup {
  val messageRate = newMeter(metricId + "MessagesPerSec",  "messages", TimeUnit.SECONDS)
  val byteRate = newMeter(metricId + "BytesPerSec",  "bytes", TimeUnit.SECONDS)
}

/**
 * Tracks metrics for each topic the given consumer client has consumed data from.
 * @param clientId The clientId of the given consumer client.
 */
class ConsumerTopicStats(clientId: String) extends Logging {
  private val valueFactory = (k: ClientIdAndTopic) => new ConsumerTopicMetrics(k)
  private val stats = new Pool[ClientIdAndTopic, ConsumerTopicMetrics](Some(valueFactory))
  private val allTopicStats = new ConsumerTopicMetrics(new ClientIdAndTopic(clientId, "AllTopics")) // to differentiate from a topic named AllTopics

  def getConsumerAllTopicStats(): ConsumerTopicMetrics = allTopicStats

  def getConsumerTopicStats(topic: String): ConsumerTopicMetrics = {
    stats.getAndMaybePut(new ClientIdAndTopic(clientId, topic + "-"))
  }
}

/**
 * Stores the topic stats information of each consumer client in a (clientId -> ConsumerTopicStats) map.
 */
object ConsumerTopicStatsRegistry {
  private val valueFactory = (k: String) => new ConsumerTopicStats(k)
  private val globalStats = new Pool[String, ConsumerTopicStats](Some(valueFactory))

  def getConsumerTopicStat(clientId: String) = {
    globalStats.getAndMaybePut(clientId)
  }

  def removeConsumerTopicStat(clientId: String) {
    globalStats.remove(clientId)
  }
}