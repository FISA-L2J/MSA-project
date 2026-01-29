package com.msa.account_service.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Component
public class FeignClientInterceptor implements RequestInterceptor {

	private static final String AUTHORIZATION_HEADER = "Authorization";

	@Override
	public void apply(RequestTemplate template) {
		ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
				.getRequestAttributes();

		if (requestAttributes != null) {
			HttpServletRequest request = requestAttributes.getRequest();
			String token = request.getHeader(AUTHORIZATION_HEADER);

			if (token != null) {
				template.header(AUTHORIZATION_HEADER, token);
				log.debug("FeignClientInterceptor: Token propagated to downstream service");
			} else {
				log.warn("FeignClientInterceptor: Authorization header not found in the current request");
			}
		}
	}
}
