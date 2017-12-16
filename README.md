#### Elasticsearch 入门
1. 依赖
>
    		<dependency>
    			<groupId>org.elasticsearch.client</groupId>
    			<artifactId>transport</artifactId>
    			<version>${elasticsearch.version}</version>
    		</dependency>
>

2. 配置client bean
>
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
>

3. 增删改查
>
    //es连接
        @Autowired
        private TransportClient client;
    
        /**
         * 查询es
         */
        @GetMapping("/get/book/novel")
        public ResponseEntity get(String id) {
            if (StringUtils.isEmpty(id)) {
                return ResponseEntity.notFound().build();
            }
            GetResponse result = client.prepareGet("book", "man", id).get();
    
            //没找到,返回404
            if (!result.isExists()) {
                return ResponseEntity.notFound().build();
            }
    
            return ResponseEntity.ok().body(result.getSource());
        }
    
        /**
         * 新增
         */
        @PostMapping("/add/book/novel")
        public ResponseEntity add(String title, String author,
                                  Integer wordCount, @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date publishDate) {
            try {
                //构造json
                XContentBuilder content = XContentFactory.jsonBuilder()
                        .startObject()
                        .field("title", title)
                        .field("author", author)
                        .field("word_count", wordCount)
                        .field("publish_date", publishDate.getTime())
                        .endObject();
                //增加并接收返回结果
                IndexResponse result = client.prepareIndex("book", "man")
                        .setSource(content)
                        .get();
                return ResponseEntity.ok().body(result.getId());
            } catch (IOException e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
    
        /**
         * 删除
         */
        @DeleteMapping("/delete/book/novel")
        public String delete(String id) {
            DeleteResponse result = client.prepareDelete("book", "man", id).get();
            return result.getResult().toString();
        }
    
        /**
         * 更新
         */
        @PutMapping("/update/book/novel")
        public String update(
                String id,
                @RequestParam(required = false) String title
        ) throws IOException, ExecutionException, InterruptedException {
    
    
            //构造json
            XContentBuilder content = XContentFactory.jsonBuilder()
                    .startObject()
                    .field("title", title)
                    .endObject();
            //更新请求
            UpdateRequest updateRequest = new UpdateRequest("book", "man", id);
            updateRequest.doc(content);
    
    
            UpdateResponse result = client.update(updateRequest).get();
            return result.getResult().toString();
        }
    
        /**
         * 复合查询
         */
        @PostMapping("/query/book/novel")
        public List<Map<String, Object>> query(String title,String author,Integer gtWordCount,Integer ltWordCount) {
            //构造布尔查询
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            boolQuery.must(QueryBuilders.matchQuery("author", author));
            boolQuery.must(QueryBuilders.matchQuery("title", title));
    
            //构造范围查询
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("word_count")
                    .from(gtWordCount).to(ltWordCount);
            //将两个查询用filter结合
            boolQuery.filter(rangeQuery);
    
            //请求体
            SearchRequestBuilder builder = client.prepareSearch("book")
                    .setTypes("man")
                    .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                    .setQuery(boolQuery)
                    .setFrom(0)
                    .setSize(10);
            //输出请求体
            System.out.println(builder);
    
            //请求并获取响应
            SearchResponse response = builder.get();
    
            List<Map<String, Object>> result = new ArrayList<>();
            for (SearchHit item : response.getHits()) {
                result.add(item.getSourceAsMap());
            }
    
            return result;
        }
>

#### Elasticsearch 安装
1. 解压
2. 创建用户
>
    useradd zx  创建名为zx的用户
    passwd zx 给zx设置密码
    su zx 切换到zx用户
    
    切换到root
    chown zx /zx/elasticsearch-6.0.0 -R 给zx权限
>

3. elasticsearch.yml 增加 network.host: 0.0.0.0

4.  max file descriptors [65535] for elasticsearch process is too low, increase to at least [65536]
root用户下 
vim /etc/security/limits.conf 
在末尾增加
>
    * soft nofile 65536
    * hard nofile 65536
    * soft nproc 2048
    * hard nproc 4096
>

5. max virtual memory areas vm.max_map_count [65530] is too low, increase to at least [262144]
vim /etc/sysctl.conf 
sysctl -p


5.启动 sh ./bin/elasticsearch
后台启动 加上 -d 或 &

6. 访问http://106.14.7.29:9200 即可看到信息

7. 停止   
JPS 看进程号  
kill  -9  进程号  或 kill -SIGTERM 进程号

#### HEAD插件安装-提供了一个elasticsearch的管理界面
* url: https://github.com/mobz/elasticsearch-head
* 下载该项目到服务器,可以直接拷贝download 的链接.然后在服务器输入
>
     wget xxxxxxx
     解压 unzip master.zip
     解压后得到 elasticsearch-head-master
>

* 此处需要安装node
>
    下载 注意,该tar.xz是编译好的文件
    wget https://nodejs.org/dist/v8.9.3/node-v8.9.3-linux-x64.tar.xz
    解压
    tar xvJf ./node-v8.9.3-linux-x64.tar.xz
    配置环境变量
    vim /etc/profile
        export NODE_HOME=xxxx
        export PATH=${NODE_HOME}/bin:$PATH
    source /etc/profile
    运行node -v 即可看到版本号
    运行npm -v 也已经OK
>

* 进入根目录 npm install 开始安装
    如有意外,可尝试使用淘宝npm镜像
>
    npm install -g cnpm --registry=https://registry.npm.taobao.org
    使用 
    cnpm install
    如果第一次失败,再试一次即可
>
* 然后 npm run start 
* 输入 http://106.14.7.29:9100/ 即可访问(如果不是中文,可以在url后面加 ?lang=zh)

* 修改elasticsearch,让插件和elasticsearch可以交互
>
    在elasticsearch根目录
    vim config/elasticsearch.yml 
    在末尾增加如下:
    http.cors.enabled: true
    http.cors.allow-origin: "*"
    其他可查看该插件的github文档
>

* 启动elasticsearch后,再启动插件
* 然后在顶部的输入框中输入 http://106.14.7.29:9200/ 点击连接即可(因为我是在其他机器访问云服务器的,它默认连接本地的9200端口)
* npm run start & 后台运行
* 关闭
>
    jobs 显示所有任务
    找到该插件的任务,前面有一个序号
    fg 序号   将该任务弄到前台
    ctrl + c 杀死即可
>

#### Elasticsearch分布式配置
* vim config/elasticsearch.yml 追加
>
    cluster.name: zx
    node.name: master 
    node.master: true
    network.host: 0.0.0.0 这句上面为了外网访问已经加了,就别加了
>

* 将其复制出一份
>
    创建新的目录
    mkdir ./es_slave 
    拷贝过去
    cp ./elasticsearch-6.0.0.tar.gz ./es_slave/
    解压
    tar -zxvf ./elasticsearch-6.0.0.tar.gz 
    修改文件
    vim ./config/elasticsearch.yml
    追加
    cluster.name: zx
    node.name: slave1 
    network.host: 0.0.0.0
    discovery.zen.ping.unicast.hosts: [127.0.0.1] # 该ip就是主节点的ip
    http.port: 9201       # 因为是在一台机器上配置,所以需要修改端口
    
    注意此处需要给新的用户权限,启动该节点
>

* 这样应该就可以了.但是我未验证,因为...服务器内存不足了

#### 基础概念
* 集群主从.  
一个Master多个Slave.  
每个集群有一个唯一的名字  cluster.name
每个节点有唯一的名字  node.name

* 索引  含有相同属性的文档集合  
类似一个数据库,例如可以存储图书索引,家具索引等
* 类型 一个索引可以定义一个或多个类型,文档必须属于一个类型 
类似一个表,例如图书索引细分为科普类/小说类等
* 文档 可以被索引的基本数据单位 
类似一行记录,例如小说类中的每本小说都是一个文档

* 分片 每个索引有多个分片,每个分片是一个Lucene索引 默认5个分片,只能在创建索引的时候指定,无法后期修改
如果只有一个索引,容易有并发压力;
分成多个,每个分片可以分摊并发.
* 备份 拷贝一份分片就完成了分片的备份 默认一个备份

#### 基本用法
* API基本格式
http://<ip>:<port>/<索引>/<类型>/<文档id>
* 常用http方法
GET/POST/POST/DELETE

* 创建索引
* 非结构化创建 索引的mappings属性为空,表示是非结构化索引
> 直接在head的控制台点击索引,然后创建  

* 结构化创建
>
    点击复合查询,输入
    {
      "novel": {
        "properties": {
          "title": {
            "type": "text"
          }
        }
      }
    }
    然后点击易读,点击验证JSON,点击提交请求
    此时,索引的mappings就会增加上如上json
    (上面其实就是发送了一个http请求,可以使用postman来进行)
>

使用postman创建.
>
    restful新增请求
    PUT请求
    指定ip,端口号,索引名
    url: 106.14.7.29:9200/people 
    选择body-raw-json格式
    输入
    {
    	"settings":{
    		"number_of_shards": 3,
    		"number_of_replicas":1
    	},
    	"mappings":{
    		"man":{
    			"properties":{
    				"name":{
    					"type":"text"
    				},
    				"country":{
    					"type":"keyword"
    				},
    				"age":{
    					"type":"integer"
    				},
    				"date":{
    					"type":"date",
    					"format":"yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"
    				}
    			}
    		},
    	}
    }
    settings表示设置分片数为3,备份数为1
    mappings也就是结构话索引的属性
    有一个man(索引的类型)有名字(text类型)/国家(关键词类型)/年龄(整型)/出生日期(日期型以及格式)等属性,;
    (本来还可以增加其他和man同级的属性,但6.X后,只支持1个类型)
>


* 插入    
指定文档id插入
>   
    PUT请求-新增
    people:索引; man:索引类型; 1:指定文档id;
    url:106.14.7.29:9200/people/man/1
    
    内容,根据之前定义的索引的man类型的属性:
    {
    	"name":"郑星",
    	"country":"China",
    	"age":30,
    	"date":"1996-04-16"
    }
    可以在head的控制台-数据浏览中看到
>

自动产生文档id插入
将上面的指定id的url改为:
106.14.7.29:9200/people/man/
请求方法改为POST即可

* 修改
>
    POST方法
    url:106.14.7.29:9200/people/man/1/_update
    内容,doc表示修改的是文档
    {
    	"doc":{
    		"name":"修改后的郑星"
    	}
    }
    或者换成如下内容,表示用脚本的方式修改,lang表示可用的脚本语言
    {
    	"script":{
    		"lang":"painless",
    		"inline":"ctx._source.age += 20"
    	}
    }
    或如下脚本
        {
        	"script":{
        		"lang":"painless",
        		"inline":"ctx._source.age = params.age",
        		"params":{
        			"age":100
        		}
        	}
        }
>

* 删除
>   
    删除people索引中man类型的文档id为1的文档
    DELETE请求
    url: 106.14.7.29:9200/people/man/1
    
    删除整个book索引,此时该索引下的所有文档都会被删除
    DELETE请求 
    url: 106.14.7.29:9200/people
>

* 查询
简单查询
>
    查询book索引,novel类型,id为1的文档
    GET
    URL:106.14.7.29:9200/book/book/1
>
条件查询
>
    查询所有文档
    POST
    URL:106.14.7.29:9200/book/_search
    {
    	"query":{
    		"match_all":{}
    	}
    }
    
    查询一条数据,from表示从哪里返回  
    {
    	"query":{
    		"match_all":{}
    	},
    	"from": 1,
    	"size": 1
    }
    
    根据作者名查询
    {
    	"query":{
    		"match":{
    			"author":"笛安"
    		}
    	}
    }
    
    根据作者名查询,并根据日期倒序排序
    {
    	"query":{
    		"match":{
    			"author":"笛安"
    		}
    	},
    	"sort":[
    		{"publish_date":{"order":"desc"}}
    	]
    }
>

聚合查询
>
    第一种
    也就是数据库中的group by.
    将所有文档根据作者分组查询. 
    group_by_word_count:可任意起名
    terms:条款,是固定的格式
    POST
    URL:106.14.7.29:9200/book/_search
    {
    	"aggs":{
    		"group_by_word_count":{
    			"terms":{
    				"field":"author"
    			}
    		}
    	}
    }
    
    返回两组聚合信息,分别根据作者和日期聚合,
    所谓的聚合就是,把相同值的属性进行统计.
    例如鲁迅有5本书.那么根据作者聚合的结果就是
    key:鲁迅, doc_count:5
    {
    	"aggs":{
    		"group_by_word_count":{
    			"terms":{
    				"field":"author"
    			}
    		},
    		"group_by_publish_date":{
    			"terms":{
    				"field":"publish_date"
    			}
    		}
    	}
    }
    
    第二种,统计计算
    统计word_count属性的相关信息
    总数/最大值/最小值/平均值/求和等
    {
    	"aggs":{
    		"grades_word_count":{
    			"stats":{
    				"field":"word_count"
    			}
    		}
    	}
    }
    
    直接获取它的最小值
    {
        	"aggs":{
        		"grades_word_count":{
        			"min":{
        				"field":"word_count"
        			}
        		}
        	}
        }
>

* 高级查询
子条件查询-特定字段查询所指特定值
>   
    Query Context
    查询过程中,除了判断文档是否满足查询条件外,es还会计算一个_score来标识匹配程度,用来判断匹配契合度.
        全文本查询 针对文本类型数据
        字段级别查询 针对结构化数据,如数字/日期等
        
        查询出标题和 java入门 相关的所有文档.  python入门这样的书名也会被查询出,不过_score比较低
                {
                	"query":{
                		"match":{
                			"title":"java入门"	
                		}
                	}
                }
                这样就只会查询出一条匹配度最高的,当然也可能为空
                {
                	"query":{
                		"match_phrase":{
                			"title":"java入门"	
                		}
                	}
                }
                
                多条件查询,只要其中一个字段中匹配了查询值即可
                {
                	"query":{
                		"multi_match":{
                			"query":"笛安",
                			"fields":["author","title"]
                		}
                	}
                }
    
    Query String 语法查询
    
    查询所有字段有和 东霓 相关值的文档
    {
    	"query":{
    		"query_string":{
    			"query":"东霓"
    		}
    	}
    }
    
    查询所有字段有和 东霓或李时珍 相关值的文档
    {
    	"query":{
    		"query_string":{
    			"query":"(东霓) OR 李时珍"
    		}
    	}
    }
    
    
    查询指定字段有和 东霓或李时珍 相关值的文档
    {
    	"query":{
    		"query_string":{
    			"query":"(东霓) OR java",
    			"fields":["author","title"]
    		}
    	}
    }
    
    {
    	"query":{
    		"term":{
    			"title":"java"
    		}
    	}
    }
    
    字数在大于等于1000,小于等于2000
    {
    	"query":{
    		"range":{
    			"word_count":{
    				"gte":1000,
    				"lte":2000
    			}
    		}
    	}
    }
    
    Filter Context
    只判断文档是否满足条件,只有yes或no
    会对结果缓存
    
    {
    	"query":{
    		"bool":{
    			"filter":{
    				"term":{
    					"title":1000
    				}
    			}
    		}
    	}
    }
>

复合条件查询-以一定的逻辑组合子条件查询 
固定分数查询
>
    固定分数(_score)的查询.如果不加constant_scoreconstant_score,每个文档的分数会不同,加了都会是同一个分数
    constant_score 只支持filter
    {
    	"query":{
    		"constant_score":{
    			"filter":{
    				"match":{
    					"title":"入门"
    				}
    			}
    		}
    	}
    }
    不仅固定分数,还指定了分数
    {
    	"query":{
    		"constant_score":{
    			"filter":{
    				"match":{
    					"title":"入门"
    				}
    			},
    			"boost":2
    		}
    	}
    }
>

布尔查询
>
    满足其中任意一个即可,相当于or
    如果把should改为must,则必须满足所有条件.相当于and
    还有must_not 一定不能满足条件
    {
        "query":{
            "bool":{
                "should":[
                    {
                        "match":{
                            "author":"笛安"
                        }
                    },
                    {
                        "match":{
                            "title":"东霓"
                        }
                    }
                    
                ]
            }
        }
    }
    
    返回结果后,在过滤出某个条件的文档
    {
    	"query":{
    		"bool":{
    			"should":[
    				{
    					"match":{
    						"author":"李时珍"
    					}
    				},
    				{
    					"match":{
    						"title":"东霓"
    					}
    				}
    			],
    			"filter":[
    				{
    					"term":{
    						"word_count":4000
    					}
    				}
    			]
    		}
    	}
    }
>



    




#### ElasticSearch spring boot stater data elasticsearch 框架 只支持2.X版本
1. 引入依赖
>
    	compile('org.springframework.boot:spring-boot-starter-data-elasticsearch')
>

2. 新建实体类
>
    /**
     * author:ZhengXing
     * datetime:2017-12-04 21:15
     * es文档类
     */
    @Document(indexName = "blog",type = "blog")
    @Data
    @NoArgsConstructor(access = AccessLevel.PROTECTED)//防止直接使用
    public class EsBlog implements Serializable{
        @Id
        private String id;//string的id
        private String title;
        private String summary;//摘要
        private String content;
    
        public EsBlog(String title, String summary, String content) {
            this.title = title;
            this.summary = summary;
            this.content = content;
        }
    }
>

3. 数据操作类:
和spring data jpa类似,都是被springData包装过的
>
    /**
     * author:ZhengXing
     * datetime:2017-12-04 21:22
     * es博客 文档
     */
    public interface EsBlogRepository extends ElasticsearchRepository<EsBlog,String> {
    
        /**
         * 查询不重复的,标题包含,或摘要包含,或正文包含该字符的分页的数据
         *
         * @return
         */
        Page<EsBlog> findDistinctEsBlogByTitleContainingOrSummaryContainingOrContentContaining(
                String title, String summary, String content, Pageable pageable
        );
    }
>

* 需要注意的是spring boot stater data elasticsearch 中整合的是2.4.6版本的elasticsearch.需要安装最低支持版本的才能操作.  
如果需要使用最新版,就无法使用spring data框架
 
 
#### IDEA配置文件修改
根目录的bin中的idea64.exe.vmoptions
>
    -Xms4096m
    -Xmx4096m
    -XX:ReservedCodeCacheSize=1024m
    -XX:+UseConcMarkSweepGC
    -XX:SoftRefLRUPolicyMSPerMB=50
    -XX:ParallelGCThreads=4
    -ea
    -Dsun.io.useCanonCaches=false
    -Djava.net.preferIPv4Stack=true
    -XX:+HeapDumpOnOutOfMemoryError
    -XX:-OmitStackTraceInFastThrow

>
