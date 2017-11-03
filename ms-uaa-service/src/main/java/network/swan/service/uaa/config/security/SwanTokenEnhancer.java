package network.swan.service.uaa.config.security;

import network.swan.core.utils.mapper.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;

/**
 * token增强器
 * Created by guanzhenxing on 2017/11/3.
 */
public class SwanTokenEnhancer implements TokenEnhancer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SwanTokenEnhancer.class);

    @Override
    public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {

        LOGGER.info(JsonMapper.toJsonString(accessToken));
        LOGGER.info(JsonMapper.toJsonString(authentication));

        return accessToken;
    }
}
