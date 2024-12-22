# apijson-iotdb  [![](https://jitpack.io/v/APIJSON/apijson-iotdb.svg)](https://jitpack.io/#APIJSON/apijson-iotdb)
腾讯 [APIJSON](https://github.com/Tencent/APIJSON) 7.0.3+ 的 IoTDB 数据库插件，可通过 Maven, Gradle 等远程依赖。<br />
An IoTDB plugin for Tencent [APIJSON](https://github.com/Tencent/APIJSON) 7.0.3+

![image](https://private-user-images.githubusercontent.com/5738175/397984593-6d088d9c-86d9-40a9-b9fa-b49c679e19a6.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3MzQ4NzQ5NzQsIm5iZiI6MTczNDg3NDY3NCwicGF0aCI6Ii81NzM4MTc1LzM5Nzk4NDU5My02ZDA4OGQ5Yy04NmQ5LTQwYTktYjlmYS1iNDljNjc5ZTE5YTYucG5nP1gtQW16LUFsZ29yaXRobT1BV1M0LUhNQUMtU0hBMjU2JlgtQW16LUNyZWRlbnRpYWw9QUtJQVZDT0RZTFNBNTNQUUs0WkElMkYyMDI0MTIyMiUyRnVzLWVhc3QtMSUyRnMzJTJGYXdzNF9yZXF1ZXN0JlgtQW16LURhdGU9MjAyNDEyMjJUMTMzNzU0WiZYLUFtei1FeHBpcmVzPTMwMCZYLUFtei1TaWduYXR1cmU9OWM2OTBmYmM4MmRiYzIwNjg4ZDAzZDkxYjkwMjE4ZDM4NWY1NDFkYTFjNTMyMWQ0MzFlMzkxODhlNDIxNDYyMCZYLUFtei1TaWduZWRIZWFkZXJzPWhvc3QifQ.POonQKkSUjGuF1fRF4vbT61mI1wKGmamLTL5Ld7GmxA)
![image](https://github.com/user-attachments/assets/920dd1ea-5490-4c1e-8132-7e1f6324f156)
![image](https://github.com/user-attachments/assets/4b65704d-67ee-403b-a1b6-cf1c16e761e0)

## 添加依赖
## Add Dependency

### Maven
#### 1. 在 pom.xml 中添加 JitPack 仓库
#### 1. Add the JitPack repository to pom.xml
```xml
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```

![image](https://user-images.githubusercontent.com/5738175/167261814-d75d8fff-0e64-4534-a840-60ef628a8873.png)

<br />

#### 2. 在 pom.xml 中添加 apijson-iotdb 依赖
#### 2. Add the apijson-iotdb dependency to pom.xml
```xml
	<dependency>
	    <groupId>com.github.APIJSON</groupId>
	    <artifactId>apijson-iotdb</artifactId>
	    <version>LATEST</version>
	</dependency>
```

<br />

https://github.com/APIJSON/APIJSON-Demo/blob/master/APIJSON-Java-Server/APIJSONBoot-MultiDataSource/pom.xml

<br />
<br />

### Gradle
#### 1. 在项目根目录 build.gradle 中最后添加 JitPack 仓库
#### 1. Add the JitPack repository in your root build.gradle at the end of repositories
```gradle
	allprojects {
		repositories {
			maven { url 'https://jitpack.io' }
		}
	}
```
<br />

#### 2. 在项目某个 module 目录(例如 `app`) build.gradle 中添加 apijson-iotdb 依赖
#### 2. Add the apijson-iotdb dependency in one of your modules(such as `app`)
```gradle
	dependencies {
	        implementation 'com.github.APIJSON:apijson-iotdb:latest'
	}
```

<br />
<br />
<br />

## 使用
## Usage

在你项目继承 AbstractSQLExecutor 的子类重写方法 execute <br/>
Override execute in your SQLExecutor extends AbstractSQLExecutor

```java
        @Override
        public JSONObject execute(@NotNull SQLConfig<Long> config, boolean unknownType) throws Exception {
            if (config.isIoTDB()) {
                return InfluxdbUtil.execute(config, null, unknownType);
            }
   
            return super.execute(config, unknownType);
        }
```

<br/>
在你项目继承 AbstractSQLConfig 的子类重写方法 execute <br/>
Override execute in your SQLConfig extends AbstractSQLConfig

```java
	@Override
    public String getTablePath() {
        return IoTDBUtil.getTablePath(super.getTablePath(), isIoTDB());
    }
```

#### 见 [IoTDBUtil](/src/main/java/apijson/iotdb/IoTDBUtil.java) 的注释及 [APIJSONBoot-MultiDataSource](https://github.com/APIJSON/APIJSON-Demo/blob/master/APIJSON-Java-Server/APIJSONBoot-MultiDataSource) 的 [DemoSQLExecutor](https://github.com/APIJSON/APIJSON-Demo/blob/master/APIJSON-Java-Server/APIJSONBoot-MultiDataSource/src/main/java/apijson/demo/DemoSQLExecutor.java) <br />

#### See document in [IoTDBUtil](/src/main/java/apijson/iotdb/IoTDBUtil.java) and [DemoSQLExecutor](https://github.com/APIJSON/APIJSON-Demo/blob/master/APIJSON-Java-Server/APIJSONBoot-MultiDataSource/src/main/java/apijson/demo/DemoSQLExecutor.java) in [APIJSONBoot-MultiDataSource](https://github.com/APIJSON/APIJSON-Demo/blob/master/APIJSON-Java-Server/APIJSONBoot-MultiDataSource)

<br />
<br />
<br />

有问题可以去 Tencent/APIJSON 提 issue <br />
https://github.com/Tencent/APIJSON/issues/36

<br /><br />

#### 点右上角 ⭐Star 支持一下，谢谢 ^_^
#### Please ⭐Star this project ^_^
https://github.com/APIJSON/apijson-iotdb
