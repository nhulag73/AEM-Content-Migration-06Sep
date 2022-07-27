package com.aem.migration.core.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aem.migration.core.aem.dto.components.AEMComponent;
import com.aem.migration.core.constants.MigrationConstants;
import com.aem.migration.core.wordpress.dto.WPComponent;
import com.aem.migration.core.wordpress.dto.WordPressPage;

public final class MigrationUtil {

	private MigrationUtil() {}

	private static final Logger log = LoggerFactory.getLogger(MigrationUtil.class);

	/**
	 * Adds the image component to page.
	 *
	 * @param wpPage the wp page
	 * @param counter the counter
	 * @param element the element
	 */
	public static WPComponent getComponent(final Element element, final Map<String, String> htmlElementToComponentMap) {

		String componentType = getComponentType(htmlElementToComponentMap, element);
		if(StringUtils.isNotBlank(componentType)) {

			WPComponent wpComponent = new WPComponent();
			wpComponent.setComponentType(componentType);
			if (StringUtils.equalsIgnoreCase(componentType, MigrationConstants.IMAGE_COMPONENT_TYPE)
					&& StringUtils.equalsIgnoreCase(element.nodeName(), MigrationConstants.DOCUMENT_ELEMENT_FIGURE)
					&& StringUtils.equalsIgnoreCase(element.parent().nodeName(),
							MigrationConstants.DOCUMENT_ELEMENT_BODY)) {

				getImageComponent(wpComponent, element);
				return wpComponent;
			}
		}
		return null;
	}

	/**
	 * Gets the component type.
	 *
	 * @param htmlElementToComponentMap the html element to component map
	 * @param element the element
	 * @return the component type
	 */
	private static String getComponentType(Map<String, String> htmlElementToComponentMap, Element element) {
		
		String parentElement = element.nodeName();
		String firstElement = element.firstElementChild() != null ? element.firstElementChild().nodeName() : "";
		String secondElement = element.firstElementChild() != null
				&& element.firstElementChild().firstElementChild() != null
						? element.firstElementChild().firstElementChild().nodeName()
						: "";
		if (htmlElementToComponentMap.containsKey(parentElement + "." + firstElement)) {

			return htmlElementToComponentMap.get(parentElement + "." + firstElement);
		} else if (htmlElementToComponentMap.containsKey(parentElement + "." + firstElement + "." + secondElement)) {

			return htmlElementToComponentMap.get(parentElement + "." + firstElement + "." + secondElement);
		}
		return null;
	}

	/**
	 * Gets the image component.
	 *
	 * @param wpComponent the wp component
	 * @param element the element
	 * @return the image component
	 */
	private static void getImageComponent(WPComponent wpComponent, Element element) {

		Elements imgElement = element.getElementsByTag(MigrationConstants.DOCUMENT_ELEMENT_FIGURE_IMAGE);
		Elements anchorElement = element.getElementsByTag(MigrationConstants.DOCUMENT_ELEMENT_FIGURE_ANCHOR);
		if(anchorElement.hasAttr("href")) {
			wpComponent.setSrc(anchorElement.attr("href"));
		}
		if(imgElement.hasAttr("src")) {
			wpComponent.setImgSrc(imgElement.attr("src"));
		}
		if(imgElement.hasAttr("height")) {
			wpComponent.setHeight(imgElement.attr("height"));
		}
		if(imgElement.hasAttr("width")) {
			wpComponent.setWidth(imgElement.attr("width"));
		}
		if(imgElement.hasAttr("alt")) {
			wpComponent.setAlt(imgElement.attr("alt"));
		}
		if(imgElement.hasAttr("loading")) {
			wpComponent.setLoading(imgElement.attr("loading"));
		}
		Elements captionElement = element.getElementsByTag(MigrationConstants.DOCUMENT_ELEMENT_FIGURE_CAPTION);
		if(captionElement != null) {
			wpComponent.setImgCaption(captionElement.text());
		}
		log.info("Image Attributes {}", imgElement);
	}

	/**
	 * Gets the AEM components list.
	 *
	 * @param wpPage the wp page
	 * @return the AEM components list
	 */
	public static List<AEMComponent> getAEMComponentsList(WordPressPage wpPage) {

		List<AEMComponent> aemComponentsList = new ArrayList<>();
		List<WPComponent> wpComponentsList = wpPage.getWpComponentsList();
		int counter = 0;
		if (CollectionUtils.isNotEmpty(wpComponentsList)) {

			while (counter < wpComponentsList.size()) {

				aemComponentsList.add(getAEMComponentObject(wpComponentsList.get(counter), counter));
				counter++;
			}
		}
		return aemComponentsList;
	}

	/**
	 * Gets the AEM component object.
	 *
	 * @param component the component
	 * @param str the str
	 * @return the AEM component object
	 */
	private static AEMComponent getAEMComponentObject(final WPComponent component, final int counter) {
		
		if(StringUtils.equalsIgnoreCase("image", component.getComponentType())) {

			return new AEMComponent(component, counter);
		}
		return null;
	}

	/**
	 * Gets the component properties map.
	 *
	 * @param compoJCRPropertyMap the compo property map
	 * @return the component properties map
	 */
	public static Map<String, String[]> getComponentJCRPropertiesMap(String[] compoPropertyMap) {
		
		Map<String, String[]> mapCompProp = new HashMap<>();
		if(compoPropertyMap != null && compoPropertyMap.length > 0) {

			for(int count=0; count < compoPropertyMap.length; count++) {

				String[] tmpStr = compoPropertyMap[count].split("=");
				if(tmpStr != null && tmpStr.length == 2) {
				
					String componentName = compoPropertyMap[count].split("=")[0];
					String[] propertyList = compoPropertyMap[count].split("=")[1].split(",");
					mapCompProp.put(componentName, propertyList);
				}
			}
		}
		return mapCompProp;
	}

	/**
	 * Gets the component JCR properties.
	 *
	 * @param aemComp the aem comp
	 * @param mapCompProp the map comp prop
	 * @return the component JCR properties
	 */
	public static String getComponentJCRProperties(AEMComponent aemComp, Map<String, String[]> mapCompProp) {
		
		if(aemComp != null) {

			StringBuilder sb = new StringBuilder();
			String componentNode = "jcr:content/root/container/container/";
			String[] properties = mapCompProp.get(aemComp.getComponentType());
			for(int count=0; count < properties.length; count++) {

				String validateProperty = getValidatedProperty(properties[count], aemComp);
				if(StringUtils.isNotBlank(validateProperty)) {

					sb.append(" -F \"").append(componentNode).append(aemComp.getNodeName());
					sb.append("/").append(properties[count]).append("=").append(validateProperty).append("\"");
				}
			}
			return sb.toString();
		}
		return null;
	}

	/**
	 * Gets the validated property.
	 *
	 * @param property the property
	 * @param aemComp the aem comp
	 * @return the validated property
	 */
	private static String getValidatedProperty(String property, AEMComponent aemComp) {
		
		//Validate later if java.lang.reflect can be used to avoid these conditions for all the properties 
		// and by adding method declaration in the same config for each properties 
		
		if(StringUtils.equalsIgnoreCase(property, "sling:resourceType")) {
			return aemComp.getSling_resourceType();
		} else if(StringUtils.equalsIgnoreCase(property, "jcr:title")) {
			return aemComp.getJcr_title();
		} else if(StringUtils.equalsIgnoreCase(property, "fileReference")) {
			return aemComp.getFileReference();
		} else if(StringUtils.equalsIgnoreCase(property, "alt")) {
			return aemComp.getAlt();
		} else if(StringUtils.equalsIgnoreCase(property, "linkURL")) {
			return aemComp.getLinkURL();
		} 
		
		return null;
	}

	/**
	 * Gets the AEM page JSON.
	 *
	 * @param aemPage the aem page
	 * @param aemComponentPropertyMapping the aem component property mapping
	 * @return the AEM page JSON
	 */
	public static String getAEMPageJSON(String aemPageJSONStr, String[] aemComponentPropertyMapping) {
		
		for(int count = 0; count < aemComponentPropertyMapping.length; count++) {

			String[] propertyMap = StringUtils.isNotBlank(aemComponentPropertyMapping[count]) ? aemComponentPropertyMapping[count].split("=") : null;
			if(propertyMap != null && propertyMap.length == 2) {
				
				aemPageJSONStr = aemPageJSONStr.replace(propertyMap[0], propertyMap[1]);
			}
		}

		return aemPageJSONStr;
	}

	/**
	 * Gets the HTML elements to AEM component map.
	 *
	 * @param htmlElementstoParseList the html elementsto parse list
	 * @return the HTML elements to AEM component map
	 */
	public static Map<String, String> getHTMLElementsToAEMComponentMap(String[] htmlElementstoParseList) {

		Map<String, String> map = new HashMap<>();
		if(htmlElementstoParseList != null && htmlElementstoParseList.length > 0) {

			for(String element : htmlElementstoParseList) {

				String[] strArray = element.split(MigrationConstants.EQUAL);
				if(strArray.length == 2) {

					map.put(strArray[0], strArray[1]);
				}
			}
		}
		return map;
	}

}
