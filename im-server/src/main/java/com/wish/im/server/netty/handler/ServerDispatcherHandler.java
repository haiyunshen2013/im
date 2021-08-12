package com.wish.im.server.netty.handler;

import com.wish.im.common.message.Message;
import com.wish.im.common.message.MsgStatus;
import com.wish.im.common.message.MsgType;
import com.wish.im.server.netty.client.ClientInfo;
import com.wish.im.server.netty.message.ReqMessage;
import com.wish.ipusher.api.context.IpusherContext;
import com.wish.ipusher.api.context.IpusherContextHolder;
import com.wish.ipusher.api.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.condition.NameValueExpression;
import org.springframework.web.reactive.result.condition.ParamsRequestCondition;
import org.springframework.web.reactive.result.condition.PatternsRequestCondition;
import org.springframework.web.reactive.result.condition.RequestMethodsRequestCondition;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.wish.im.common.message.MsgType.RESPONSE;
import static com.wish.im.server.netty.handler.ServerHandler.CLIENT_ATTR;

/**
 * 描述
 *
 * @author shy
 * @since 2021/7/30
 */
@Component
@AllArgsConstructor
@ChannelHandler.Sharable
@Slf4j
public class ServerDispatcherHandler extends SimpleChannelInboundHandler<Message> {
    private static final Pattern QUERY_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        ReqMessage reqMessage = convertReqMsg(msg);
        HandlerMethod handlerMethod = getMatchingHandlerMethod(requestMappingHandlerMapping.getHandlerMethods(), reqMessage);
        Message.Header header = new Message.Header();
        header.setFromId("server");
        header.setMsgType(MsgType.RESPONSE);
        header.setToId(msg.getHeader().getFromId());
        byte[] body = null;
        if (handlerMethod != null) {
            HandlerMethod resolvedBean = handlerMethod.createWithResolvedBean();
            header.setStatus(MsgStatus.OK.getValue());
            Object invoke;
            try {
                IpusherContext ipusherContext = new IpusherContext();
                ipusherContext.setAdmin(true);
                Channel channel = ctx.channel();
                ClientInfo clientInfo = ((ClientInfo) channel.attr(CLIENT_ATTR).get());
                ipusherContext.setServiceId(clientInfo.getId());
                ipusherContext.setUid(clientInfo.getId());
                IpusherContextHolder.setContext(ipusherContext);
                invoke = invokeMethHandler(msg, reqMessage, resolvedBean);
                body = JsonUtils.serializeAsBytes(invoke);
            } catch (IllegalAccessException | InvocationTargetException e){
                header.setStatus(MsgStatus.INTERNAL_SERVER_ERROR.getValue());
            }finally {
                IpusherContextHolder.release();
            }
        } else {
            // 没有找到合适的处理器
            header.setStatus(MsgStatus.NOT_FOUND.getValue());
        }
        Message message = new Message(header, body);
        ctx.channel().writeAndFlush(message);
    }

    private Object invokeMethHandler(Message msg, ReqMessage reqMessage, HandlerMethod resolvedBean) throws IllegalAccessException, InvocationTargetException {
        Object invoke;
        Method method = resolvedBean.getMethod();
        MethodParameter[] methodParameters = resolvedBean.getMethodParameters();
        if (methodParameters.length == 0) {
            invoke = method.invoke(resolvedBean.getBean());
        } else {
            boolean isFirst = false;
            MultiValueMap<String, String> ext = reqMessage.getQueryParams();
            Object[] paramArr = new Object[methodParameters.length];
            for (int i = 0; i < methodParameters.length; i++) {
                MethodParameter methodParameter = methodParameters[i];
                if (methodParameter.hasParameterAnnotation(RequestBody.class)) {
                    if (msg.getBody() == null || msg.getBody().length < 2 || isFirst) {
                        break;
                    }
                    isFirst = true;
                    Object deserialize = JsonUtils.deserialize(msg.getBody(), methodParameter.getParameterType());
                    paramArr[i] = deserialize;
                } else {
                    Class<?> parameterType = methodParameter.getParameterType();
                    Object value;
                    if (!parameterType.isArray() && !Collection.class.isAssignableFrom(parameterType)) {
                        value = ext.get(methodParameter.getParameter().getName()).isEmpty() ? null : ext.get(methodParameter.getParameter().getName()).get(0);
                    } else {
                        value = ext.get(methodParameter.getParameter().getName());
                    }
                    Object o = JsonUtils.convertValue(value, parameterType);
                    paramArr[i] = o;
                }
            }
            invoke = method.invoke(resolvedBean.getBean(), paramArr);
        }
        return invoke;
    }

    private ReqMessage convertReqMsg(Message msg) {
        ReqMessage reqMessage = new ReqMessage();
        reqMessage.setMessage(msg);
        Message.Header header = msg.getHeader();
        reqMessage.setUri(URI.create(header.getUrl()));
        reqMessage.setQueryParams(initQueryParams(reqMessage));
        return reqMessage;
    }

    private HandlerMethod getMatchingHandlerMethod(Map<RequestMappingInfo, HandlerMethod> handlerMethods, ReqMessage message) {
        for (RequestMappingInfo handlerMethod : handlerMethods.keySet()) {
            RequestMethodsRequestCondition methods = matchRequestMethod(handlerMethod, message);
            if (methods == null) {
                continue;
            }
            ParamsRequestCondition params = matchParamsRequest(handlerMethod, message);
            if (params == null) {
                continue;
            }
            PatternsRequestCondition patterns = matchPatternsRequest(handlerMethod, message);
            if (patterns == null) {
                continue;
            }
            return handlerMethods.get(handlerMethod);
        }
        return null;
    }

    private RequestMethodsRequestCondition matchRequestMethod(RequestMappingInfo handlerMethod, ReqMessage message) {
        RequestMethodsRequestCondition methodsCondition = handlerMethod.getMethodsCondition();
        String method = message.getHeader().getMethod();
        if (methodsCondition.getMethods().isEmpty()) {
            return methodsCondition;
        }
        if (StringUtils.isBlank(method)) {
            return null;
        }
        return new RequestMethodsRequestCondition(RequestMethod.valueOf(method));
    }

    private ParamsRequestCondition matchParamsRequest(RequestMappingInfo handlerMethod, ReqMessage message) {
        ParamsRequestCondition paramsCondition = handlerMethod.getParamsCondition();
        Set<NameValueExpression<String>> expressions = paramsCondition.getExpressions();
        if (expressions.isEmpty()) {
            return paramsCondition;
        }
        for (NameValueExpression<String> expression : expressions) {
            if (message.getQueryParams().containsKey(expression.getName())) {
                return paramsCondition;
            }
        }
        return null;
    }

    private PatternsRequestCondition matchPatternsRequest(RequestMappingInfo handlerMethod, ReqMessage message) {
        String url = message.getHeader().getUrl();
        int endIndex = url.indexOf('?');
        url = endIndex < 0 ? url : url.substring(0, endIndex);
        PathContainer pathContainer = PathContainer.parsePath(url);
        SortedSet<PathPattern> pathPatterns = new TreeSet<>();
        PatternsRequestCondition patternsCondition = handlerMethod.getPatternsCondition();
        Set<PathPattern> patterns = patternsCondition.getPatterns();
        if (patterns.isEmpty()) {
            return patternsCondition;
        }
        for (PathPattern pattern : patterns) {
            if (pattern.matches(pathContainer)) {
                pathPatterns.add(pattern);
            }
        }
        return pathPatterns.isEmpty() ? null : new PatternsRequestCondition(pathPatterns.toArray(new PathPattern[0]));
    }


    protected MultiValueMap<String, String> initQueryParams(ReqMessage reqMessage) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        String query = reqMessage.getUri().getRawQuery();
        if (query != null) {
            Matcher matcher = QUERY_PATTERN.matcher(query);
            while (matcher.find()) {
                String name = decodeQueryParam(matcher.group(1));
                String eq = matcher.group(2);
                String value = matcher.group(3);
                value = (value != null ? decodeQueryParam(value) : (org.springframework.util.StringUtils.hasLength(eq) ? "" : null));
                queryParams.add(name, value);
            }
        }
        return queryParams;
    }

    @SuppressWarnings("deprecation")
    private String decodeQueryParam(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            if (log.isWarnEnabled()) {
                log.warn("Could not decode query value [" + value + "] as 'UTF-8'. " +
                        "Falling back on default encoding: " + ex.getMessage());
            }
            return URLDecoder.decode(value);
        }
    }
}
