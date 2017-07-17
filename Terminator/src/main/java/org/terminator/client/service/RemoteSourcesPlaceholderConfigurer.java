package org.terminator.client.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionVisitor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.StringValueResolver;

public class RemoteSourcesPlaceholderConfigurer implements BeanFactoryPostProcessor,ApplicationListener<ContextRefreshedEvent>{

	private String beanName;

	private BeanFactory beanFactory;
	
	private Map<String,Integer> useMap = new HashMap<String,Integer>();
	
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactoryToProcess) throws BeansException {
		
		BeanDefinitionVisitor visitor = new BeanDefinitionVisitor(new RemoteConfigStringValueResolver());

		String[] beanNames = beanFactoryToProcess.getBeanDefinitionNames();
		for (String curName : beanNames) {
			// Check that we're not parsing our own bean definition,
			// to avoid failing on unresolvable placeholders in properties file locations.
			if (!(curName.equals(this.beanName) && beanFactoryToProcess.equals(this.beanFactory))) {
				BeanDefinition bd = beanFactoryToProcess.getBeanDefinition(curName);
				try {
					visitor.visitBeanDefinition(bd);
				}
				catch (Exception ex) {
					throw new BeanDefinitionStoreException(bd.getResourceDescription(), curName, ex.getMessage(), ex);
				}
			}
		}
		
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if(!useMap.isEmpty()){
			
		}
	}
	
	private class RemoteConfigStringValueResolver implements StringValueResolver{

		@Override
		public String resolveStringValue(String strVal) {
			return null;
		}
		
	}
	
	

}
