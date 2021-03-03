package gov.cms.dpc.common.logging;

import ch.qos.logback.classic.pattern.ThrowableHandlingConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextVO;
import com.google.common.collect.Maps;
import io.dropwizard.logging.json.EventAttribute;
import io.dropwizard.logging.json.layout.JsonFormatter;
import io.dropwizard.logging.json.layout.TimestampFormatter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DPCJsonLayoutTest {

    private DPCJsonLayout dpcJsonLayout;

    @Mock
    private ILoggingEvent loggingEvent;

    @Mock
    private LoggerContextVO loggerContextVO;

    @Mock
    private JsonFormatter jsonFormatter;

    @Mock
    private TimestampFormatter timestampFormatter;

    @Mock
    private ThrowableHandlingConverter throwableHandlingConverter;

    @Before
    public void setup() {
        dpcJsonLayout = new DPCJsonLayout(jsonFormatter,
                timestampFormatter,
                throwableHandlingConverter,
                Set.of(EventAttribute.MESSAGE),
                new HashMap<String, String>(),
                new HashMap<String, Object>(),
                new HashSet<>(),
                false
                );
        when(loggerContextVO.getName()).thenReturn("name");
        when(loggingEvent.getLoggerContextVO()).thenReturn(loggerContextVO);
    }

    @Test
    public void noChangeWhenMessageNotParseableAsMap() {
        String message = "hello I'm not parsable";
        when(loggingEvent.getFormattedMessage()).thenReturn(message);
        Map<String, Object> map = dpcJsonLayout.toJsonMap(loggingEvent);
        Assert.assertEquals(message, map.get("message"));

        message = "key1=value2, key2=value2, key3NoVal";
        when(loggingEvent.getFormattedMessage()).thenReturn(message);
        map = dpcJsonLayout.toJsonMap(loggingEvent);
        Assert.assertEquals(message, map.get("message"));
    }

    @Test
    public void changeWhenMessageIsParsableAsMap() {
        String message = "key1=value1, key2=value2";

        when(loggingEvent.getFormattedMessage()).thenReturn(message);
        Map<String, Object> map = dpcJsonLayout.toJsonMap(loggingEvent);
        Assert.assertFalse(map.containsKey("message"));
        Assert.assertEquals("value1", map.get("key1"));
        Assert.assertEquals("value2", map.get("key2"));
    }

    @Test
    public void testLookBackLogJsonParsing() {
        String message = "billingPeriodDate=Thu Jul 01 00:00:00 UTC 1999, lookBackDate=Thu Aug 27 00:43:30 UTC 2020, monthsDifference=253, eobProvider=null, eobCareTeamProviders=999999999999;9999999999, jobProvider=1232125215, eobOrganization=9999999999, jobOrganization=5808156785, withinLimit=false, eobProviderMatch=false, eobOrganizationMatch=false";
        when(loggingEvent.getFormattedMessage()).thenReturn(message);
        Map<String, Object> map = dpcJsonLayout.toJsonMap(loggingEvent);
        Assert.assertFalse(map.containsKey("message"));
        Assert.assertEquals("999999999999;9999999999", map.get("eobCareTeamProviders"));
    }

    @Test
    public void testMBIMasking() {
        Map<String,String> inputOutputMap = Maps.newHashMap();
        inputOutputMap.put("1SQ3F00AA00", "***MBI?***");
        inputOutputMap.put("mbi1SQ3F00AA00", "mbi***MBI?***");
        inputOutputMap.put(" 1SQ3F00AA00", " ***MBI?***");
        inputOutputMap.put("1SQ3F00AA00 ", "***MBI?*** ");
        inputOutputMap.put("rAnDoM1SQ3F00AA00", "rAnDoM***MBI?***");
        inputOutputMap.put("1SQ3F00AA00rANDom1", "***MBI?***rANDom1");
        inputOutputMap.put("11SQ3F00AA00", "1***MBI?***");
        inputOutputMap.put("random text 1SQ3F00AA00", "random text ***MBI?***");
        inputOutputMap.put("21SQ3F00AA002", "2***MBI?***2");
        inputOutputMap.put("", "");
        inputOutputMap.put(" ", " ");

        inputOutputMap.entrySet().stream().forEach(entry -> {
            final String unMaskedMessage = entry.getKey();
            final String expectedMaskedMessage = entry.getValue();
            when(loggingEvent.getFormattedMessage()).thenReturn(unMaskedMessage);
            Map<String, Object> map = dpcJsonLayout.toJsonMap(loggingEvent);
            Assert.assertEquals(expectedMaskedMessage, map.get("message"));
        });
    }

    @Test
    public void testMBIMaskingWhenMessageIsParsableAsMap() {
        final String message = "key1=value1, key2=1SQ3F00AA00, key3=value3";

        when(loggingEvent.getFormattedMessage()).thenReturn(message);
        Map<String, Object> map = dpcJsonLayout.toJsonMap(loggingEvent);
        Assert.assertFalse(map.containsKey("message"));
        Assert.assertEquals("value1", map.get("key1"));
        Assert.assertEquals("***MBI?***", map.get("key2"));
        Assert.assertEquals("value3", map.get("key3"));
    }
}
