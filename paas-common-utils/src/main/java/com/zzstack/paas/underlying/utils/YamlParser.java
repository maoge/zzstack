package com.zzstack.paas.underlying.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.zzstack.paas.underlying.utils.paas.ServiceConfigTemplate;

public class YamlParser {

	private static final Logger log = LoggerFactory.getLogger(YamlParser.class);

	private Yaml yaml = null;
	private String yamlFile = null;
	private Map<String, Object> props = null;

	public YamlParser(String yamlFile) {
		this.yamlFile = yamlFile;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> parseMap() throws Exception {
		File file = new File(yamlFile);

		try {
			this.yaml = new Yaml();

			InputStream inStream = null;
			if (file.exists()) {
				inStream = openInputStream(file);
			} else {
				inStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(yamlFile);
			}

			if (inStream != null) {
				props = (Map<String, Object>) yaml.loadAs(inStream, Map.class);
			}
		} catch (Exception e) {
			log.error("读取配置文件:{}失败", yamlFile, e);
		}

		return props;
	}

	public Object parseObject(Class<?> clazz) {
		File file = new File(yamlFile);
		Object obj = null;

		try {
			this.yaml = new Yaml();

			InputStream inStream = null;
			if (file.exists()) {
				inStream = openInputStream(file);
			} else {
				inStream = ServiceConfigTemplate.class.getResourceAsStream(yamlFile);
				if (inStream == null) {
				    inStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(yamlFile);
				}
			}

			if (inStream != null) {
			    obj = yaml.loadAs(inStream, clazz);
			}

		} catch (Exception e) {
			log.error("读取配置文件{}失败", yamlFile, e);
		}

		return obj;
	}

	public static FileInputStream openInputStream(final File file) throws IOException {
		if (file.exists()) {
			if (file.isDirectory()) {
				throw new IOException("File '" + file + "' exists but is a directory");
			}
			if (file.canRead() == false) {
				throw new IOException("File '" + file + "' cannot be read");
			}
		} else {
			throw new FileNotFoundException("File '" + file + "' does not exist");
		}
		return new FileInputStream(file);
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private void printProps(Map<String, Object> map, int count) {
		Set<Entry<String, Object>> entries = map.entrySet();
		for (Entry<String, Object> entry : entries) {
			String key = entry.getKey();
			Object value = entry.getValue();

			for (int i = 0; i < count; i++) {
				System.out.print("    ");
			}

			if (value instanceof Map) {
				System.out.println(key + ":");
				printProps((Map<String, Object>) value, count + 1);
			} else if (value instanceof List) {
				System.out.println(key + ":");
				for (Object obj : (List<?>) value) {
					for (int i = 0; i < count; i++) {
						System.out.print("    ");
					}
					System.out.println("    - " + obj.toString());
				}
			} else {
				System.out.println(key + ": " + value);
			}
		}
	}

}
