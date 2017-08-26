package network.swan.frame.db.datasource;

import com.google.common.collect.Maps;
import network.swan.core.utils.ExceptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 动态数据源注册<br/>
 * 启动动态数据源请在启动类中（如SpringBootSampleApplication）
 * 添加 @Import(DynamicDataSourceRegister.class)
 * <p>
 * <br/>
 * 默认数据源：
 * <pre>
 *    spring.datasource.url=""
 *    spring.datasource.username=""
 *    spring.datasource.password=""
 *    spring.datasource.driver-class-name=""
 * </pre>
 * <p>
 * 新增数据源配置：
 * <pre>
 *    spring.custom.datasource.names=ds1,ds2
 *    spring.custom.datasource.ds1.driver-class-name=""
 *    spring.custom.datasource.ds1.url=""
 *    spring.custom.datasource.ds1.username=""
 *    spring.custom.datasource.ds1.password=""
 *
 *    spring.custom.datasource.ds2.driver-class-name=""
 *    spring.custom.datasource.ds2.url=""
 *    spring.custom.datasource.ds2.username=""
 *    spring.custom.datasource.ds2.password=""
 * </pre>
 */
public class DynamicDataSourceRegister implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicDataSourceRegister.class);

    private ConversionService conversionService = new DefaultConversionService();
    private PropertyValues dataSourcePropertyValues;

    // 如配置文件中未指定数据源类型，使用该默认值
    private static final Object DATASOURCE_TYPE_DEFAULT = "com.zaxxer.hikari.HikariDataSource";

    // 数据源
    private DataSource defaultDataSource;
    private Map<String, DataSource> customDataSources = new HashMap<>();


    /**
     * 加载多数据源配置
     */
    @Override
    public void setEnvironment(Environment environment) {
        initDefaultDataSource(environment);
        initCustomDataSources(environment);
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Map<Object, Object> targetDataSources = new HashMap<>();

        // 将主数据源添加到目标数据源中
        targetDataSources.put("dataSource", defaultDataSource);
        DynamicDataSourceContextHolder.dataSourceNames.add("dataSource");

        // 添加更多数据源到目标数据源中
        targetDataSources.putAll(customDataSources);
        for (String key : customDataSources.keySet()) {
            DynamicDataSourceContextHolder.dataSourceNames.add(key);
        }

        createDynamicDataSource(registry, targetDataSources);// 创建DynamicDataSource

        LOGGER.info("Dynamic DataSource Registry");
    }

    /**
     * 初始化默认数据源
     */
    private void initDefaultDataSource(Environment environment) {
        RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(environment, "spring.datasource.");
        Map<String, Object> dsProps = Maps.newHashMap();
        dsProps.put("type", propertyResolver.getProperty("type"));
        dsProps.put("driver-class-name", propertyResolver.getProperty("driver-class-name"));
        dsProps.put("url", propertyResolver.getProperty("url"));
        dsProps.put("username", propertyResolver.getProperty("username"));
        dsProps.put("password", propertyResolver.getProperty("password"));

        defaultDataSource = buildDataSource(dsProps);

        dataBinder(defaultDataSource, environment);
    }

    /**
     * 初始化更多数据源
     */
    private void initCustomDataSources(Environment environment) {
        RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(environment, "spring.custom.datasource.");
        for (String dsPrefix : propertyResolver.getProperty("names").split(",")) {// 多个数据源
            Map<String, Object> dsProps = propertyResolver.getSubProperties(dsPrefix + ".");

            DataSource ds = buildDataSource(dsProps);
            customDataSources.put(dsPrefix, ds);

            dataBinder(ds, environment);
        }
    }

    public DataSource buildDataSource(Map<String, Object> dsProps) {
        try {
            Object type = dsProps.get("type");
            if (type == null) {
                type = DATASOURCE_TYPE_DEFAULT;// 默认DataSource
            }

            Class<? extends DataSource> dataSourceType = (Class<? extends DataSource>) Class.forName(type.toString());

            String driverClassName = dsProps.get("driver-class-name").toString();
            String url = dsProps.get("url").toString();
            String username = dsProps.get("username").toString();
            String password = dsProps.get("password").toString();

            DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create().driverClassName(driverClassName).url(url)
                    .username(username).password(password).type(dataSourceType);
            return dataSourceBuilder.build();

        } catch (ClassNotFoundException e) {
            ExceptionUtil.throwException(e);
        }
        return null;
    }


    /**
     * 为DataSource绑定更多数据
     */
    private void dataBinder(DataSource dataSource, Environment env) {
        RelaxedDataBinder dataBinder = new RelaxedDataBinder(dataSource);
        dataBinder.setConversionService(conversionService);
        dataBinder.setIgnoreNestedProperties(false);
        dataBinder.setIgnoreInvalidFields(false);
        dataBinder.setIgnoreUnknownFields(true);

        if (dataSourcePropertyValues == null) {
            Map<String, Object> tmpMap = new RelaxedPropertyResolver(env, "spring.datasource").getSubProperties(".");
            Map<String, Object> values = new HashMap<>(tmpMap);
            // 排除已经设置的属性
            values.remove("type");
            values.remove("driver-class-name");
            values.remove("url");
            values.remove("username");
            values.remove("password");
            dataSourcePropertyValues = new MutablePropertyValues(values);
        }
        dataBinder.bind(dataSourcePropertyValues);
    }

    private void createDynamicDataSource(BeanDefinitionRegistry registry, Map<Object, Object> targetDataSources) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(DynamicDataSource.class);
        beanDefinition.setSynthetic(true);
        MutablePropertyValues mpv = beanDefinition.getPropertyValues();
        mpv.addPropertyValue("defaultDataSource", defaultDataSource);
        mpv.addPropertyValue("targetDataSources", targetDataSources);
        registry.registerBeanDefinition("dataSource", beanDefinition);
    }

}