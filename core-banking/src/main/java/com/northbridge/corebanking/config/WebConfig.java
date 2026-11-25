package com.northbridge.corebanking.config;

import com.northbridge.corebanking.service.SloMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {

    @Autowired
    private SloMetrics sloMetrics;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SloInterceptor(sloMetrics)).addPathPatterns("/api/v1/**");
    }

    static class SloInterceptor implements HandlerInterceptor {
        private static final String START = SloInterceptor.class.getName() + ".start";
        private final SloMetrics metrics;

        SloInterceptor(SloMetrics metrics) {
            this.metrics = metrics;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            request.setAttribute(START, System.currentTimeMillis());
            return true;
        }

        @Override
        public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView mav) {
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            Object start = request.getAttribute(START);
            Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            if (start == null || pattern == null) {
                return;
            }
            int status = ex != null ? 500 : response.getStatus();
            metrics.record(request.getMethod() + " " + pattern, System.currentTimeMillis() - (Long) start, status);
        }
    }
}
