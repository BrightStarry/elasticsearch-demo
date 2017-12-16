package com.zx.elasticsearch.config;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * author:ZhengXing
 * datetime:2017-12-16 22:26
 */
@Configuration
public class Config {

    /**
     * 构造es连接的客户端
     */
    @Bean
    public TransportClient transportClient() throws UnknownHostException {
        //此处可构造并传入多个es地址,也就是一整个集群的所有节点
        //构造一个地址对象,查看源码得知,ip使用4个字节的字节数组传入
        //此处的端口号是es的TCP端口号,默认为9300
        TransportAddress node = new TransportAddress(
                InetAddress.getByAddress(new byte[]{106,14,7,29}),9300
        );

        Settings settings = Settings.builder()
                .put("cluster.name", "zx")
                .build();
        //如果settings为空,可以使用Settings.EMPTY
        //但是不传入settings,会无法访问
        TransportClient client = new PreBuiltTransportClient(settings);
        client.addTransportAddress(node);
        return client;
    }
}
