package com.rssreader;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RSSLoadUtility {
	boolean cancel = false;

	public void setCancel(boolean cancel) {
		this.cancel = cancel;
	}

	public List<Message> loadRSS(String url, HttpGet getMethod,
			DefaultHttpClient client) throws Exception {
		getMethod = new HttpGet(url);
		String responseBody;

		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		responseBody = client.execute(getMethod, responseHandler);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		List<Message> messages = new ArrayList<Message>();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document dom = builder.parse(new ByteArrayInputStream(responseBody
				.getBytes()));
		Element root = dom.getDocumentElement();
		NodeList items = root.getElementsByTagName("item");

		for (int i = 0; i < items.getLength() && !cancel; i++) {
			Message message = new Message();
			Node item = items.item(i);
			NodeList properties = item.getChildNodes();

			for (int j = 0; j < properties.getLength(); j++) {
				Node property = properties.item(j);
				String name = property.getNodeName();
				if (name.equalsIgnoreCase("title")) {
					message.setTitle(property.getFirstChild().getNodeValue());
				} else if (name.equalsIgnoreCase("link")) {
					message.setLink(property.getFirstChild().getNodeValue());
				} else if (name.equalsIgnoreCase("description")) {
					StringBuilder text = new StringBuilder();
					NodeList chars = property.getChildNodes();

					for (int k = 0; k < chars.getLength(); k++) {
						text.append(chars.item(k).getNodeValue());
					}

					message.setDescription(text.toString());
				} else if (name.equalsIgnoreCase("pubDate")) {
					message.setDate(property.getFirstChild().getNodeValue());
				}
			}

			message.setUpdated(false);
			messages.add(message);
		}

		return messages;
	}
}
