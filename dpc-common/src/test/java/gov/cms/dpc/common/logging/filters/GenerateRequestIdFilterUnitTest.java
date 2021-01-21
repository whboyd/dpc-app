package gov.cms.dpc.common.logging.filters;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import gov.cms.dpc.common.Constants;
import gov.cms.dpc.common.MDCConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.UUID;
import org.mockito.*;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import org.slf4j.MDC;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.UriInfo;
import static org.junit.jupiter.api.Assertions.*;

public class GenerateRequestIdFilterUnitTest {
    private GenerateRequestIdFilter filter;

    @Mock
    ContainerRequestContext mockContext;

    @BeforeEach
    public void setUp(){
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRequestIdIsGeneratedWhenMissingInHeader() throws IOException {
        String requestId = UUID.randomUUID().toString();
        Mockito.when(mockContext.getHeaderString(ArgumentMatchers.eq(Constants.DPC_REQUEST_ID_HEADER))).thenReturn(requestId);
        UriInfo mockUriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(mockUriInfo.getPath()).thenReturn("v1/Patients");
        Mockito.when(mockContext.getUriInfo()).thenReturn(mockUriInfo);
        Mockito.when(mockContext.getMethod()).thenReturn("GET");

        Logger logger = (Logger) LoggerFactory.getLogger(GenerateRequestIdFilter.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        try {
            listAppender.start();
            logger.addAppender(listAppender);

            //Without request-id in header and extraction enabled
            Mockito.when(mockContext.getHeaderString(ArgumentMatchers.eq(Constants.DPC_REQUEST_ID_HEADER))).thenReturn(null);
            filter = new GenerateRequestIdFilter(true);
            filter.filter(mockContext);
            assertEquals("resource_requested=v1/Patients, method=GET", listAppender.list.get(0).getFormattedMessage());
            assertNotNull(MDC.get(MDCConstants.DPC_REQUEST_ID));
            UUID.fromString(MDC.get(MDCConstants.DPC_REQUEST_ID));

            //Without request-id in header and extraction disabled
            Mockito.when(mockContext.getHeaderString(ArgumentMatchers.eq(Constants.DPC_REQUEST_ID_HEADER))).thenReturn(null);
            filter = new GenerateRequestIdFilter(false);
            filter.filter(mockContext);
            assertEquals("resource_requested=v1/Patients, method=GET", listAppender.list.get(0).getFormattedMessage());
            assertNotNull(MDC.get(MDCConstants.DPC_REQUEST_ID));
            UUID.fromString(MDC.get(MDCConstants.DPC_REQUEST_ID));
        } finally {
            listAppender.stop();
        }
    }

    @Test
    public void testRequestIdIsNotExtractedWhenExtractionDisabled() throws IOException {
        String requestId = UUID.randomUUID().toString();
        Mockito.when(mockContext.getHeaderString(ArgumentMatchers.eq(Constants.DPC_REQUEST_ID_HEADER))).thenReturn(requestId);
        UriInfo mockUriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(mockUriInfo.getPath()).thenReturn("v1/Patients");
        Mockito.when(mockContext.getUriInfo()).thenReturn(mockUriInfo);
        Mockito.when(mockContext.getMethod()).thenReturn("GET");

        Logger logger = (Logger) LoggerFactory.getLogger(GenerateRequestIdFilter.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        try {
            listAppender.start();
            logger.addAppender(listAppender);

            //With request-id in header and use header value Disabled
            filter = new GenerateRequestIdFilter(false);
            filter.filter(mockContext);
            assertEquals("resource_requested=v1/Patients, method=GET", listAppender.list.get(0).getFormattedMessage());
            assertNotEquals(requestId, MDC.get(MDCConstants.DPC_REQUEST_ID));
        } finally {
            listAppender.stop();
        }
    }

    @Test
    public void testRequestIdIsExtractedWhenExtractionIsEnabled() throws IOException {
        String requestId = UUID.randomUUID().toString();
        Mockito.when(mockContext.getHeaderString(ArgumentMatchers.eq(Constants.DPC_REQUEST_ID_HEADER))).thenReturn(requestId);
        UriInfo mockUriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(mockUriInfo.getPath()).thenReturn("v1/Patients");
        Mockito.when(mockContext.getUriInfo()).thenReturn(mockUriInfo);
        Mockito.when(mockContext.getMethod()).thenReturn("GET");

        Logger logger = (Logger) LoggerFactory.getLogger(GenerateRequestIdFilter.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        try {
            listAppender.start();
            logger.addAppender(listAppender);

            //With request-id in header and use header value enabled
            filter = new GenerateRequestIdFilter(true);
            filter.filter(mockContext);
            assertEquals("resource_requested=v1/Patients, method=GET", listAppender.list.get(0).getFormattedMessage());
            assertEquals(requestId, MDC.get(MDCConstants.DPC_REQUEST_ID));
        } finally {
            listAppender.stop();
        }
    }

    @Test
    public void testMdcIsBeingCleared() throws IOException {
        String requestId = UUID.randomUUID().toString();
        Mockito.when(mockContext.getHeaderString(ArgumentMatchers.eq(Constants.DPC_REQUEST_ID_HEADER))).thenReturn(requestId);
        UriInfo mockUriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(mockUriInfo.getPath()).thenReturn("v1/Patients");
        Mockito.when(mockContext.getUriInfo()).thenReturn(mockUriInfo);
        Mockito.when(mockContext.getMethod()).thenReturn("GET");

        MDC.put("Some-Key", "some-value");
        assertNotNull(MDC.get("Some-Key"));
        filter = new GenerateRequestIdFilter(true);
        filter.filter(mockContext);
        assertNull(MDC.get("Some-Key"));
    }
}