/*
 * MIT License
 *
 * Copyright (c) 2020 Airbyte
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.airbyte.workers.protocols.airbyte;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.airbyte.commons.json.Jsons;
import io.airbyte.config.JobSyncConfig.NamespaceDefinitionType;
import io.airbyte.protocol.models.AirbyteMessage;
import io.airbyte.protocol.models.CatalogHelpers;
import io.airbyte.protocol.models.ConfiguredAirbyteCatalog;
import io.airbyte.protocol.models.Field;
import io.airbyte.protocol.models.Field.JsonSchemaPrimitive;
import org.junit.jupiter.api.Test;

class NamespacingMapperTest {

  private static final String INPUT_NAMESPACE = "source_namespace";
  private static final String OUTPUT_PREFIX = "output_";
  private static final String STREAM_NAME = "user_preferences";
  private static final String FIELD_NAME = "favorite_color";
  private static final ConfiguredAirbyteCatalog CATALOG = CatalogHelpers.createConfiguredAirbyteCatalog(
      STREAM_NAME,
      INPUT_NAMESPACE,
      Field.of(FIELD_NAME, JsonSchemaPrimitive.STRING));
  private static final AirbyteMessage RECORD_MESSAGE = createRecordMessage();

  private static AirbyteMessage createRecordMessage() {
    AirbyteMessage message = AirbyteMessageUtils.createRecordMessage(STREAM_NAME, FIELD_NAME, "blue");
    message.getRecord().withNamespace(INPUT_NAMESPACE);
    return message;
  }

  @Test
  void testSourceNamespace() {
    final NamespacingMapper mapper = new NamespacingMapper(NamespaceDefinitionType.SOURCE, null, OUTPUT_PREFIX);

    final ConfiguredAirbyteCatalog originalCatalog = Jsons.clone(CATALOG);
    final ConfiguredAirbyteCatalog expectedCatalog = CatalogHelpers.createConfiguredAirbyteCatalog(
        OUTPUT_PREFIX + STREAM_NAME,
        INPUT_NAMESPACE,
        Field.of(FIELD_NAME, JsonSchemaPrimitive.STRING));
    final ConfiguredAirbyteCatalog actualCatalog = mapper.mapCatalog(CATALOG);

    assertEquals(originalCatalog, CATALOG);
    assertEquals(expectedCatalog, actualCatalog);

    final AirbyteMessage originalMessage = Jsons.clone(RECORD_MESSAGE);
    final AirbyteMessage expectedMessage = AirbyteMessageUtils.createRecordMessage(OUTPUT_PREFIX + STREAM_NAME, FIELD_NAME, "blue");
    expectedMessage.getRecord().withNamespace(INPUT_NAMESPACE);
    final AirbyteMessage actualMessage = mapper.mapMessage(RECORD_MESSAGE);

    assertEquals(originalMessage, RECORD_MESSAGE);
    assertEquals(expectedMessage, actualMessage);
  }

  @Test
  void testEmptySourceNamespace() {
    final NamespacingMapper mapper = new NamespacingMapper(NamespaceDefinitionType.SOURCE, null, OUTPUT_PREFIX);

    final ConfiguredAirbyteCatalog originalCatalog = Jsons.clone(CATALOG);
    assertEquals(originalCatalog, CATALOG);
    originalCatalog.getStreams().get(0).getStream().withNamespace(null);
    final ConfiguredAirbyteCatalog expectedCatalog = CatalogHelpers.createConfiguredAirbyteCatalog(
        OUTPUT_PREFIX + STREAM_NAME,
        null,
        Field.of(FIELD_NAME, JsonSchemaPrimitive.STRING));
    final ConfiguredAirbyteCatalog actualCatalog = mapper.mapCatalog(originalCatalog);

    assertEquals(expectedCatalog, actualCatalog);

    final AirbyteMessage originalMessage = Jsons.clone(RECORD_MESSAGE);
    assertEquals(originalMessage, RECORD_MESSAGE);
    originalMessage.getRecord().withNamespace(null);

    final AirbyteMessage expectedMessage = AirbyteMessageUtils.createRecordMessage(OUTPUT_PREFIX + STREAM_NAME, FIELD_NAME, "blue");
    expectedMessage.getRecord().withNamespace(null);
    final AirbyteMessage actualMessage = mapper.mapMessage(originalMessage);

    assertEquals(expectedMessage, actualMessage);
  }

  @Test
  void testDestinationNamespace() {
    final NamespacingMapper mapper = new NamespacingMapper(NamespaceDefinitionType.DESTINATION, null, OUTPUT_PREFIX);

    final ConfiguredAirbyteCatalog originalCatalog = Jsons.clone(CATALOG);
    final ConfiguredAirbyteCatalog expectedCatalog = CatalogHelpers.createConfiguredAirbyteCatalog(
        OUTPUT_PREFIX + STREAM_NAME,
        null,
        Field.of(FIELD_NAME, JsonSchemaPrimitive.STRING));
    final ConfiguredAirbyteCatalog actualCatalog = mapper.mapCatalog(CATALOG);

    assertEquals(originalCatalog, CATALOG);
    assertEquals(expectedCatalog, actualCatalog);

    final AirbyteMessage originalMessage = Jsons.clone(RECORD_MESSAGE);
    final AirbyteMessage expectedMessage = AirbyteMessageUtils.createRecordMessage(OUTPUT_PREFIX + STREAM_NAME, FIELD_NAME, "blue");
    final AirbyteMessage actualMessage = mapper.mapMessage(RECORD_MESSAGE);

    assertEquals(originalMessage, RECORD_MESSAGE);
    assertEquals(expectedMessage, actualMessage);
  }

  @Test
  void testCustomFormatWithVariableNamespace() {
    final NamespacingMapper mapper = new NamespacingMapper(NamespaceDefinitionType.CUSTOMFORMAT, "${SOURCE_NAMESPACE}_suffix", OUTPUT_PREFIX);

    final String expectedNamespace = INPUT_NAMESPACE + "_suffix";
    final ConfiguredAirbyteCatalog originalCatalog = Jsons.clone(CATALOG);
    final ConfiguredAirbyteCatalog expectedCatalog = CatalogHelpers.createConfiguredAirbyteCatalog(
        OUTPUT_PREFIX + STREAM_NAME, expectedNamespace,
        Field.of(FIELD_NAME, JsonSchemaPrimitive.STRING));
    final ConfiguredAirbyteCatalog actualCatalog = mapper.mapCatalog(CATALOG);

    assertEquals(originalCatalog, CATALOG);
    assertEquals(expectedCatalog, actualCatalog);

    final AirbyteMessage originalMessage = Jsons.clone(RECORD_MESSAGE);
    final AirbyteMessage expectedMessage = AirbyteMessageUtils.createRecordMessage(OUTPUT_PREFIX + STREAM_NAME, FIELD_NAME, "blue");
    expectedMessage.getRecord().withNamespace(expectedNamespace);
    final AirbyteMessage actualMessage = mapper.mapMessage(RECORD_MESSAGE);

    assertEquals(originalMessage, RECORD_MESSAGE);
    assertEquals(expectedMessage, actualMessage);
  }

  @Test
  void testCustomFormatWithoutVariableNamespace() {
    final NamespacingMapper mapper = new NamespacingMapper(NamespaceDefinitionType.CUSTOMFORMAT, "output", OUTPUT_PREFIX);

    final String expectedNamespace = "output";
    final ConfiguredAirbyteCatalog originalCatalog = Jsons.clone(CATALOG);
    final ConfiguredAirbyteCatalog expectedCatalog = CatalogHelpers.createConfiguredAirbyteCatalog(
        OUTPUT_PREFIX + STREAM_NAME, expectedNamespace,
        Field.of(FIELD_NAME, JsonSchemaPrimitive.STRING));
    final ConfiguredAirbyteCatalog actualCatalog = mapper.mapCatalog(CATALOG);

    assertEquals(originalCatalog, CATALOG);
    assertEquals(expectedCatalog, actualCatalog);

    final AirbyteMessage originalMessage = Jsons.clone(RECORD_MESSAGE);
    final AirbyteMessage expectedMessage = AirbyteMessageUtils.createRecordMessage(OUTPUT_PREFIX + STREAM_NAME, FIELD_NAME, "blue");
    expectedMessage.getRecord().withNamespace(expectedNamespace);
    final AirbyteMessage actualMessage = mapper.mapMessage(RECORD_MESSAGE);

    assertEquals(originalMessage, RECORD_MESSAGE);
    assertEquals(expectedMessage, actualMessage);
  }

  @Test
  void testEmptyCustomFormatWithVariableNamespace() {
    final NamespacingMapper mapper = new NamespacingMapper(NamespaceDefinitionType.CUSTOMFORMAT, "${SOURCE_NAMESPACE}", OUTPUT_PREFIX);

    final ConfiguredAirbyteCatalog originalCatalog = Jsons.clone(CATALOG);
    assertEquals(originalCatalog, CATALOG);
    originalCatalog.getStreams().get(0).getStream().withNamespace(null);
    final ConfiguredAirbyteCatalog expectedCatalog = CatalogHelpers.createConfiguredAirbyteCatalog(
        OUTPUT_PREFIX + STREAM_NAME,
        null,
        Field.of(FIELD_NAME, JsonSchemaPrimitive.STRING));
    final ConfiguredAirbyteCatalog actualCatalog = mapper.mapCatalog(originalCatalog);

    assertEquals(expectedCatalog, actualCatalog);

    final AirbyteMessage originalMessage = Jsons.clone(RECORD_MESSAGE);
    assertEquals(originalMessage, RECORD_MESSAGE);
    originalMessage.getRecord().withNamespace(null);

    final AirbyteMessage expectedMessage = AirbyteMessageUtils.createRecordMessage(OUTPUT_PREFIX + STREAM_NAME, FIELD_NAME, "blue");
    expectedMessage.getRecord().withNamespace(null);
    final AirbyteMessage actualMessage = mapper.mapMessage(originalMessage);

    assertEquals(expectedMessage, actualMessage);
  }

  @Test
  void testEmptyPrefix() {
    final NamespacingMapper mapper = new NamespacingMapper(NamespaceDefinitionType.SOURCE, null, null);

    final ConfiguredAirbyteCatalog originalCatalog = Jsons.clone(CATALOG);
    final ConfiguredAirbyteCatalog expectedCatalog = CatalogHelpers.createConfiguredAirbyteCatalog(
        STREAM_NAME,
        INPUT_NAMESPACE,
        Field.of(FIELD_NAME, JsonSchemaPrimitive.STRING));
    final ConfiguredAirbyteCatalog actualCatalog = mapper.mapCatalog(CATALOG);

    assertEquals(originalCatalog, CATALOG);
    assertEquals(expectedCatalog, actualCatalog);

    final AirbyteMessage originalMessage = Jsons.clone(RECORD_MESSAGE);
    final AirbyteMessage expectedMessage = AirbyteMessageUtils.createRecordMessage(
        STREAM_NAME,
        FIELD_NAME, "blue");
    expectedMessage.getRecord().withNamespace(INPUT_NAMESPACE);
    final AirbyteMessage actualMessage = mapper.mapMessage(RECORD_MESSAGE);

    assertEquals(originalMessage, RECORD_MESSAGE);
    assertEquals(expectedMessage, actualMessage);
  }

}
