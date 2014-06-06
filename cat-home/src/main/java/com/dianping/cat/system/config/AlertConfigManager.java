package com.dianping.cat.system.config;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.unidal.dal.jdbc.DalNotFoundException;
import org.unidal.helper.Files;
import org.unidal.lookup.annotation.Inject;
import org.unidal.lookup.util.StringUtils;

import com.dianping.cat.Cat;
import com.dianping.cat.core.config.Config;
import com.dianping.cat.core.config.ConfigDao;
import com.dianping.cat.core.config.ConfigEntity;
import com.dianping.cat.home.alert.config.entity.AlertConfig;
import com.dianping.cat.home.alert.config.entity.Receiver;
import com.dianping.cat.home.alert.config.transform.DefaultSaxParser;

public class AlertConfigManager implements Initializable {

	@Inject
	private ConfigDao m_configDao;

	private int m_configId;

	private AlertConfig m_config;

	private static final String CONFIG_NAME = "alertConfig";

	public Map<String, Receiver> buildIdToReceiverMap() {
		Map<String, Receiver> map = new HashMap<String, Receiver>();

		for (Receiver receiver : m_config.getReceivers()) {
			map.put(receiver.getId(), receiver);
		}

		return map;
	}

	public String buildReceiverContentByOnOff(String originXml, String allOnOrOff) {
		try {
			if (StringUtils.isEmpty(allOnOrOff)) {
				return originXml;
			}
			
			AlertConfig tmpConfig = DefaultSaxParser.parse(originXml);

			if (allOnOrOff.equals("on")) {
				turnOnOrOffConfig(tmpConfig, true);
			} else if (allOnOrOff.equals("off")) {
				turnOnOrOffConfig(tmpConfig, false);
			}
			
			return tmpConfig.toString();
		} catch (Exception e) {
			Cat.logError(e);
			return null;
		}
   }

	public AlertConfig getAlertConfig() {
		return m_config;
	}

	public Receiver getReceiverById(String id) {
		return buildIdToReceiverMap().get(id);
	}
	
	@Override
	public void initialize() throws InitializationException {
		try {
			Config config = m_configDao.findByName(CONFIG_NAME, ConfigEntity.READSET_FULL);
			String content = config.getContent();

			m_config = DefaultSaxParser.parse(content);
			m_configId = config.getId();
		} catch (DalNotFoundException e) {
			try {
				String content = Files.forIO().readFrom(
				      this.getClass().getResourceAsStream("/config/default-alert-config.xml"), "utf-8");
				Config config = m_configDao.createLocal();

				config.setName(CONFIG_NAME);
				config.setContent(content);
				m_configDao.insert(config);

				m_config = DefaultSaxParser.parse(content);
				m_configId = config.getId();
			} catch (Exception ex) {
				Cat.logError(ex);
			}
		} catch (Exception e) {
			Cat.logError(e);
		}
		if (m_config == null) {
			m_config = new AlertConfig();
		}
	}

	public boolean insert(String xml) {
		try {
			m_config = DefaultSaxParser.parse(xml);
			
			return storeConfig();
		} catch (Exception e) {
			Cat.logError(e);
			return false;
		}
	}

	private boolean storeConfig() {
		synchronized (this) {
			try {
				Config config = m_configDao.createLocal();

				config.setId(m_configId);
				config.setKeyId(m_configId);
				config.setName(CONFIG_NAME);
				config.setContent(m_config.toString());
				m_configDao.updateByPK(config, ConfigEntity.UPDATESET_FULL);
			} catch (Exception e) {
				Cat.logError(e);
				return false;
			}
		}
		return true;
	}

	private void turnOnOrOffConfig(AlertConfig config, boolean isOn) {
		for (Receiver receiver : config.getReceivers()) {
			receiver.setEnable(isOn);
		}
	}

}
