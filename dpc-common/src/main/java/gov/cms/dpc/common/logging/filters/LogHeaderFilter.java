package gov.cms.dpc.common.logging.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;

public class LogHeaderFilter implements ContainerRequestFilter {
	private static final Logger logger = LoggerFactory.getLogger(LogHeaderFilter.class);
	final String headerKey;

	public LogHeaderFilter(String headerKey) { this.headerKey = headerKey; }

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		String headerValue = requestContext.getHeaderString(headerKey);

		// Escape commas so they don't get confused with field separators
		if(headerValue != null) {
			headerValue = headerValue.replace(",", "\\,");
		}

		logger.info("{}={}", headerKey, headerValue);
	}
}
