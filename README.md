#sdfasdfads
#第二节 Spring Boot连接Mysql<br>
##1、新建一个Spring Boot项目，选择web、mysql支持.<br>
##2、数据源的配置（有两种方法）<br>
###方案一、使用Spring Boot默认配置<br>
使用Spring Boot默认配置，不需要创建dataSource和jdbcTemplate的Bean<br>

在application.properties中配置数据源信息

```
spring.datasource.driver-class-name=com.mysql.jdbc.Driver
spring.datasource.url=jdbc:mysql://localhost:3306/springboot_db
spring.datasource.username=root
spring.datasource.password=root

```

方案二、手动创建

在src/main/resource/config/source.properties中配置如数据源信息

```
source.driverClassName = com.mysql.jdbc.Driver
source.url = jdbc:mysql://localhost:3306/springboot_db
source.username = root
source.password = root
```

通过Java Config创建dataSource和jdbcTemplate


```
@Configuration
@EnableTransactionManagement
@PropertySource(value = {"classpath:config/source.properties"})
public class BeanConfig {
 
    @Autowired
    private Environment env;
 
    @Bean(destroyMethod = "close")
    public DataSource dataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName(env.getProperty("source.driverClassName").trim());
        dataSource.setUrl(env.getProperty("source.url").trim());
        dataSource.setUsername(env.getProperty("source.username").trim());
        dataSource.setPassword(env.getProperty("source.password").trim());
        return dataSource;
    }
 
    @Bean
    public JdbcTemplate jdbcTemplate() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate();
        jdbcTemplate.setDataSource(dataSource());
        return jdbcTemplate;
    }
}

```

<font color="red">注意：第二种方案时，需要把注解都写上，否则会连不上数据库，尤其@Configuration
DruidDataSource为com.alibaba.druid下面的实现类
</font>


##3、数据库的创建

执行如下sql脚本

```
CREATE DATABASE /*!32312 IF NOT EXISTS*/`springboot_db` /*!40100 DEFAULT CHARACTER SET utf8 */;
 
USE `springboot_db`;
 
DROP TABLE IF EXISTS `t_author`;
 
CREATE TABLE `t_author` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `real_name` varchar(32) NOT NULL COMMENT '用户名称',
  `nick_name` varchar(32) NOT NULL COMMENT '用户匿名',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;
```


##4、新建domian包，并在下面建立Author实体
```
public class Author {
    private Long id;
    private String realName;
    private String nickName;
    // SET和GET方法
}
```
##5、新建dao包，并在下面建立接口AuthorDao及其实现类AuthorDaoImpl

```
public interface AuthorDao {
    int add(Author author);
    int update(Author author);
    int delete(Long id);
    Author findAuthor(Long id);
    List<Author> findAuthorList();
}
```
```
@Repository
public class AuthorDaoImpl implements AuthorDao {
 
    @Autowired
    private JdbcTemplate jdbcTemplate;
 
    @Override
    public int add(Author author) {
        return jdbcTemplate.update("insert into t_author(real_name, nick_name) values(?, ?)", 
                author.getRealName(), author.getNickName());
    }
 
    @Override
    public int update(Author author) {
        return jdbcTemplate.update("update t_author set real_name = ?, nick_name = ? where id = ?", 
                new Object[]{author.getRealName(), author.getNickName(), author.getId()});      
    }
 
    @Override
    public int delete(Long id) {
        return jdbcTemplate.update("delete from t_author where id = ?", id);
    }
 
    @Override
    public Author findAuthor(Long id) {
        List<Author> list = jdbcTemplate.query("select * from t_author where id = ?", new Object[]{id}, new BeanPropertyRowMapper(Author.class));
        if(null != list && list.size()>0){
            Author auhtor = list.get(0);
            return auhtor;
        }else{
            return null;
        }
    }
    @Override
    public List<Author> findAuthorList() {
        List<Author> list = jdbcTemplate.query("select * from t_author", new Object[]{}, new BeanPropertyRowMapper<Author>(Author.class));
        return list;
    }
}
```

<font color="red">注意：dao层的实现类的注解为@Repository</font>

##6、新建service包，并在其下建立AuthorService接口及其实现类AuthorServiceImpl.

```
public interface AuthorService {
    int add(Author author);
    int update(Author author);
    int delete(Long id);
    Author findAuthor(Long id);
    List<Author> findAuthorList();
}
```

```
@Service("authorService")
public class AuthorServiceImpl implements AuthorService {
    @Autowired
    private AuthorDao authorDao;
 
    @Override
    public int add(Author author) {
        return this.authorDao.add(author);
    }
 
    @Override
    public int update(Author author) {
        return this.authorDao.update(author);      
    }
 
    @Override
    public int delete(Long id) {
        return this.authorDao.delete(id);
    }
 
    @Override
    public Author findAuthor(Long id) {
        return this.authorDao.findAuthor(id);
    }
 
    @Override
    public List<Author> findAuthorList() {
        return this.authorDao.findAuthorList();
    }
}
```

<font color="red">注意：service层实现类的注解为@Service</font>

##7、新建controller包及Controller类

此时采用RESTful API 接口进行测试
注意使用的方法及请求路径

例如查询用户信息的方法

请求路径/data/jdbc/author/数字

请求方法为method = RequestMethod.GET

```
@RestController
@RequestMapping(value="/data/jdbc/author")
public class AuthorController {
  @Autowired
  private AuthorService authorService;
  /**
   * 查询用户列表
   */
  @RequestMapping(method = RequestMethod.GET)
  public Map<String,Object> getAuthorList(HttpServletRequest request) {        
    List<Author> authorList = this.authorService.findAuthorList();
    Map<String,Object> param = new HashMap<String,Object>();
    param.put("total", authorList.size());
    param.put("rows", authorList);
    return param;
  }
  /**
   * 查询用户信息
   */
  @RequestMapping(value = "/{userId:\\d+}", method = RequestMethod.GET)
  public Author getAuthor(@PathVariable Long userId, HttpServletRequest request) {
    Author author = this.authorService.findAuthor(userId);
    if(author == null){
        throw new RuntimeException("查询错误");
    }
    return author;
  }
 
  /**
   * 新增方法
   */
  @RequestMapping(method = RequestMethod.POST)
  public void add(@RequestBody JSONObject jsonObject) {
    String userId = jsonObject.getString("user_id");
    String realName = jsonObject.getString("real_name");
    String nickName = jsonObject.getString("nick_name");
    Author author = new Author();
    if (author!=null) {
        author.setId(Long.valueOf(userId));
    }
    author.setRealName(realName);
    author.setNickName(nickName);
    try{
        this.authorService.add(author);
    }catch(Exception e){
        e.printStackTrace();
        throw new RuntimeException("新增错误");
    }
  }
  /**
   * 更新方法
   */
  @RequestMapping(value = "/{userId:\\d+}", method = RequestMethod.PUT)
    public void update(@PathVariable Long userId, @RequestBody JSONObject jsonObject) {
    Author author = this.authorService.findAuthor(userId);
    String realName = jsonObject.getString("real_name");
    String nickName = jsonObject.getString("nick_name");
    author.setRealName(realName);
    author.setNickName(nickName);
    try{
        this.authorService.update(author);
    }catch(Exception e){
        e.printStackTrace();
        throw new RuntimeException("更新错误");
    } 
  }
  /**
   * 删除方法
   */
  @RequestMapping(value = "/{userId:\\d+}", method = RequestMethod.DELETE)
    public void delete(@PathVariable Long userId) {
    try{
        this.authorService.delete(userId);
    }catch(Exception e){
        throw new RuntimeException("删除错误");
    }
  }
}
```

<font color="red">注意：使用restful的风格时，需要使用@RestController而不能是Controller

其中Application.java中可以有@RestController

JsonObject包为fastJson的类
</font>

