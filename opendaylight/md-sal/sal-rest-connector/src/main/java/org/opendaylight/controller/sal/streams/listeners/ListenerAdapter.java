package org.opendaylight.controller.sal.streams.listeners;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.internal.ConcurrentSet;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;

import javax.activation.UnsupportedDataTypeException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.opendaylight.controller.md.sal.common.api.data.DataChangeEvent;
import org.opendaylight.controller.sal.core.api.data.DataChangeListener;
import org.opendaylight.controller.sal.rest.impl.XmlMapper;
import org.opendaylight.controller.sal.restconf.impl.ControllerContext;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeWithValue;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * {@link ListenerAdapter} is responsible to track events, which occurred by
 * changing data in data source.
 */
public class ListenerAdapter implements DataChangeListener {

	private static final Logger logger = LoggerFactory
			.getLogger(ListenerAdapter.class);
	private final XmlMapper xmlMapper = new XmlMapper();
	private final SimpleDateFormat rfc3339 = new SimpleDateFormat(
			"yyyy-MM-dd'T'hh:mm:ssZ");

	private final InstanceIdentifier path;
	private ListenerRegistration<DataChangeListener> registration;
	private final String streamName;
	private Set<Channel> subscribers = new ConcurrentSet<>();
	private final EventBus eventBus;
	private final EventBusChangeRecorder eventBusChangeRecorder;

	/**
	 * Creates new {@link ListenerAdapter} listener specified by path and stream
	 * name.
	 * 
	 * @param path
	 *            Path to data in data store.
	 * @param streamName
	 *            The name of the stream.
	 */
	ListenerAdapter(InstanceIdentifier path, String streamName) {
		Preconditions.checkNotNull(path);
		Preconditions
				.checkArgument(streamName != null && !streamName.isEmpty());
		this.path = path;
		this.streamName = streamName;
		eventBus = new AsyncEventBus(Executors.newSingleThreadExecutor());
		eventBusChangeRecorder = new EventBusChangeRecorder();
		eventBus.register(eventBusChangeRecorder);
	}

	@Override
	public void onDataChanged(
			DataChangeEvent<InstanceIdentifier, CompositeNode> change) {
		if (!change.getCreatedConfigurationData().isEmpty()
				|| !change.getCreatedOperationalData().isEmpty()
				|| !change.getUpdatedConfigurationData().isEmpty()
				|| !change.getUpdatedOperationalData().isEmpty()
				|| !change.getRemovedConfigurationData().isEmpty()
				|| !change.getRemovedOperationalData().isEmpty()) {
			String xml = prepareXmlFrom(change);
			Event event = new Event(EventType.NOTIFY);
			event.setData(xml);
			eventBus.post(event);
		}
	}

	/**
	 * Tracks events of data change by customer.
	 */
	private final class EventBusChangeRecorder {
		@Subscribe
		public void recordCustomerChange(Event event) {
			if (event.getType() == EventType.REGISTER) {
				Channel subscriber = event.getSubscriber();
				if (!subscribers.contains(subscriber)) {
					subscribers.add(subscriber);
				}
			} else if (event.getType() == EventType.DEREGISTER) {
				subscribers.remove(event.getSubscriber());
				Notificator
						.removeListenerIfNoSubscriberExists(ListenerAdapter.this);
			} else if (event.getType() == EventType.NOTIFY) {
				for (Channel subscriber : subscribers) {
					if (subscriber.isActive()) {
						logger.debug("Data are sent to subscriber {}:",
								subscriber.remoteAddress());
						subscriber.writeAndFlush(new TextWebSocketFrame(event
								.getData()));
					} else {
						logger.debug(
								"Subscriber {} is removed - channel is not active yet.",
								subscriber.remoteAddress());
						subscribers.remove(subscriber);
					}
				}
			}
		}
	}

	/**
	 * Represents event of specific {@link EventType} type, holds data and
	 * {@link Channel} subscriber.
	 */
	private final class Event {
		private final EventType type;
		private Channel subscriber;
		private String data;

		/**
		 * Creates new event specified by {@link EventType} type.
		 * 
		 * @param type
		 *            EventType
		 */
		public Event(EventType type) {
			this.type = type;
		}

		/**
		 * Gets the {@link Channel} subscriber.
		 * 
		 * @return Channel
		 */
		public Channel getSubscriber() {
			return subscriber;
		}

		/**
		 * Sets subscriber for event.
		 * 
		 * @param subscriber
		 *            Channel
		 */
		public void setSubscriber(Channel subscriber) {
			this.subscriber = subscriber;
		}

		/**
		 * Gets event data.
		 * 
		 * @return String representation of event data.
		 */
		public String getData() {
			return data;
		}

		/**
		 * Sets event data.
		 * 
		 * @param String
		 *            data.
		 */
		public void setData(String data) {
			this.data = data;
		}

		/**
		 * Gets event type.
		 * 
		 * @return The type of the event.
		 */
		public EventType getType() {
			return type;
		}
	}

	/**
	 * Type of the event.
	 */
	private enum EventType {
		REGISTER, DEREGISTER, NOTIFY;
	}

	/**
	 * Prepare data in printable form and transform it to String.
	 * 
	 * @param change
	 *            DataChangeEvent
	 * @return Data in printable form.
	 */
	private String prepareXmlFrom(
			DataChangeEvent<InstanceIdentifier, CompositeNode> change) {
		Document doc = createDocument();
		Element notificationElement = doc.createElementNS(
				"urn:ietf:params:xml:ns:netconf:notification:1.0",
				"notification");
		doc.appendChild(notificationElement);

		Element eventTimeElement = doc.createElement("eventTime");
		eventTimeElement.setTextContent(toRFC3339(new Date()));
		notificationElement.appendChild(eventTimeElement);

		Element dataChangedNotificationEventElement = doc.createElementNS(
				"urn:opendaylight:params:xml:ns:yang:controller:md:sal:remote",
				"data-changed-notification");
		addValuesToDataChangedNotificationEventElement(doc,
				dataChangedNotificationEventElement, change);
		notificationElement.appendChild(dataChangedNotificationEventElement);

		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer
					.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(
					"{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.transform(new DOMSource(doc), new StreamResult(
					new OutputStreamWriter(out, "UTF-8")));
			byte[] charData = out.toByteArray();
			return new String(charData, "UTF-8");
		} catch (TransformerException | UnsupportedEncodingException e) {
			String msg = "Error during transformation of Document into String";
			logger.error(msg, e);
			return msg;
		}
	}

	/**
	 * Formats data specified by RFC3339.
	 * 
	 * @param d
	 *            Date
	 * @return Data specified by RFC3339.
	 */
	private String toRFC3339(Date d) {
		return rfc3339.format(d).replaceAll("(\\d\\d)(\\d\\d)$", "$1:$2");
	}

	/**
	 * Creates {@link Document} document.
	 * 
	 * @return {@link Document} document.
	 */
	private Document createDocument() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document doc = null;
		try {
			DocumentBuilder bob = dbf.newDocumentBuilder();
			doc = bob.newDocument();
		} catch (ParserConfigurationException e) {
			return null;
		}
		return doc;
	}

	/**
	 * Adds values to data changed notification event element.
	 * 
	 * @param doc
	 *            {@link Document}
	 * @param dataChangedNotificationEventElement
	 *            {@link Element}
	 * @param change
	 *            {@link DataChangeEvent}
	 */
	private void addValuesToDataChangedNotificationEventElement(Document doc,
			Element dataChangedNotificationEventElement,
			DataChangeEvent<InstanceIdentifier, CompositeNode> change) {
		addValuesFromDataToElement(doc, change.getCreatedConfigurationData(),
				dataChangedNotificationEventElement, Store.CONFIG,
				Operation.CREATED);
		addValuesFromDataToElement(doc, change.getCreatedOperationalData(),
				dataChangedNotificationEventElement, Store.OPERATION,
				Operation.CREATED);
		if (change.getCreatedConfigurationData().isEmpty()) {
			addValuesFromDataToElement(doc,
					change.getUpdatedConfigurationData(),
					dataChangedNotificationEventElement, Store.CONFIG,
					Operation.UPDATED);
		}
		if (change.getCreatedOperationalData().isEmpty()) {
			addValuesFromDataToElement(doc, change.getUpdatedOperationalData(),
					dataChangedNotificationEventElement, Store.OPERATION,
					Operation.UPDATED);
		}
		addValuesFromDataToElement(doc, change.getRemovedConfigurationData(),
				dataChangedNotificationEventElement, Store.CONFIG,
				Operation.DELETED);
		addValuesFromDataToElement(doc, change.getRemovedOperationalData(),
				dataChangedNotificationEventElement, Store.OPERATION,
				Operation.DELETED);
	}

	/**
	 * Adds values from data to element.
	 * 
	 * @param doc
	 *            {@link Document}
	 * @param data
	 *            Set of {@link InstanceIdentifier}.
	 * @param element
	 *            {@link Element}
	 * @param store
	 *            {@link Store}
	 * @param operation
	 *            {@link Operation}
	 */
	private void addValuesFromDataToElement(Document doc,
			Set<InstanceIdentifier> data, Element element, Store store,
			Operation operation) {
		if (data == null || data.isEmpty()) {
			return;
		}
		for (InstanceIdentifier path : data) {
			Node node = createDataChangeEventElement(doc, path, null, store,
					operation);
			element.appendChild(node);
		}
	}

	/**
	 * Adds values from data to element.
	 * 
	 * @param doc
	 *            {@link Document}
	 * @param data
	 *            Map of {@link InstanceIdentifier} and {@link CompositeNode}.
	 * @param element
	 *            {@link Element}
	 * @param store
	 *            {@link Store}
	 * @param operation
	 *            {@link Operation}
	 */
	private void addValuesFromDataToElement(Document doc,
			Map<InstanceIdentifier, CompositeNode> data, Element element,
			Store store, Operation operation) {
		if (data == null || data.isEmpty()) {
			return;
		}
		for (Entry<InstanceIdentifier, CompositeNode> entry : data.entrySet()) {
			Node node = createDataChangeEventElement(doc, entry.getKey(),
					entry.getValue(), store, operation);
			element.appendChild(node);
		}
	}

	/**
	 * Creates changed event element from data.
	 * 
	 * @param doc
	 *            {@link Document}
	 * @param path
	 *            Path to data in data store.
	 * @param data
	 *            {@link CompositeNode}
	 * @param store
	 *            {@link Store}
	 * @param operation
	 *            {@link Operation}
	 * @return {@link Node} node represented by changed event element.
	 */
	private Node createDataChangeEventElement(Document doc,
			InstanceIdentifier path, CompositeNode data, Store store,
			Operation operation) {
		Element dataChangeEventElement = doc.createElement("data-change-event");

		Element pathElement = doc.createElement("path");
		addPathAsValueToElement(path, pathElement);
		dataChangeEventElement.appendChild(pathElement);

		Element storeElement = doc.createElement("store");
		storeElement.setTextContent(store.value);
		dataChangeEventElement.appendChild(storeElement);

		Element operationElement = doc.createElement("operation");
		operationElement.setTextContent(operation.value);
		dataChangeEventElement.appendChild(operationElement);

		if (data != null) {
			Element dataElement = doc.createElement("data");
			Node dataAnyXml = translateToXml(path, data);
			Node adoptedNode = doc.adoptNode(dataAnyXml);
			dataElement.appendChild(adoptedNode);
			dataChangeEventElement.appendChild(dataElement);
		}

		return dataChangeEventElement;
	}

	/**
	 * Translates {@link CompositeNode} data to XML format.
	 * 
	 * @param path
	 *            Path to data in data store.
	 * @param data
	 *            {@link CompositeNode}
	 * @return Data in XML format.
	 */
	private Node translateToXml(InstanceIdentifier path, CompositeNode data) {
		DataNodeContainer schemaNode = ControllerContext.getInstance()
				.getDataNodeContainerFor(path);
		if (schemaNode == null) {
			logger.info(
					"Path '{}' contains node with unsupported type (supported type is Container or List) or some node was not found.",
					path);
			return null;
		}
		try {
			Document xml = xmlMapper.write(data, schemaNode);
			return xml.getFirstChild();
		} catch (UnsupportedDataTypeException e) {
			logger.error(
					"Error occured during translation of notification to XML.",
					e);
			return null;
		}
	}

	/**
	 * Adds path as value to element.
	 * 
	 * @param path
	 *            Path to data in data store.
	 * @param element
	 *            {@link Element}
	 */
	private void addPathAsValueToElement(InstanceIdentifier path,
			Element element) {
		// Map< key = namespace, value = prefix>
		Map<String, String> prefixes = new HashMap<>();
		InstanceIdentifier instanceIdentifier = path;
		StringBuilder textContent = new StringBuilder();
		for (PathArgument pathArgument : instanceIdentifier.getPath()) {
			textContent.append("/");
			writeIdentifierWithNamespacePrefix(element, textContent,
					pathArgument.getNodeType(), prefixes);
			if (pathArgument instanceof NodeIdentifierWithPredicates) {
				Map<QName, Object> predicates = ((NodeIdentifierWithPredicates) pathArgument)
						.getKeyValues();
				for (QName keyValue : predicates.keySet()) {
					String predicateValue = String.valueOf(predicates
							.get(keyValue));
					textContent.append("[");
					writeIdentifierWithNamespacePrefix(element, textContent,
							keyValue, prefixes);
					textContent.append("='");
					textContent.append(predicateValue);
					textContent.append("'");
					textContent.append("]");
				}
			} else if (pathArgument instanceof NodeWithValue) {
				textContent.append("[.='");
				textContent.append(((NodeWithValue) pathArgument).getValue());
				textContent.append("'");
				textContent.append("]");
			}
		}
		element.setTextContent(textContent.toString());
	}

	/**
	 * Writes identifier that consists of prefix and QName.
	 * 
	 * @param element
	 *            {@link Element}
	 * @param textContent
	 *            StringBuilder
	 * @param qName
	 *            QName
	 * @param prefixes
	 *            Map of namespaces and prefixes.
	 */
	private static void writeIdentifierWithNamespacePrefix(Element element,
			StringBuilder textContent, QName qName, Map<String, String> prefixes) {
		String namespace = qName.getNamespace().toString();
		String prefix = prefixes.get(namespace);
		if (prefix == null) {
			prefix = qName.getPrefix();
			if (prefix == null || prefix.isEmpty()
					|| prefixes.containsValue(prefix)) {
				prefix = generateNewPrefix(prefixes.values());
			}
		}

		element.setAttribute("xmlns:" + prefix, namespace.toString());
		textContent.append(prefix);
		prefixes.put(namespace, prefix);

		textContent.append(":");
		textContent.append(qName.getLocalName());
	}

	/**
	 * Generates new prefix which consists of four random characters <a-z>.
	 * 
	 * @param prefixes
	 *            Collection of prefixes.
	 * @return New prefix which consists of four random characters <a-z>.
	 */
	private static String generateNewPrefix(Collection<String> prefixes) {
		StringBuilder result = null;
		Random random = new Random();
		do {
			result = new StringBuilder();
			for (int i = 0; i < 4; i++) {
				int randomNumber = 0x61 + (Math.abs(random.nextInt()) % 26);
				result.append(Character.toChars(randomNumber));
			}
		} while (prefixes.contains(result.toString()));

		return result.toString();
	}

	/**
	 * Gets path pointed to data in data store.
	 * 
	 * @return Path pointed to data in data store.
	 */
	public InstanceIdentifier getPath() {
		return path;
	}

	/**
	 * Sets {@link ListenerRegistration} registration.
	 * 
	 * @param registration
	 *            ListenerRegistration<DataChangeListener>
	 */
	public void setRegistration(
			ListenerRegistration<DataChangeListener> registration) {
		this.registration = registration;
	}

	/**
	 * Gets the name of the stream.
	 * 
	 * @return The name of the stream.
	 */
	public String getStreamName() {
		return streamName;
	}

	/**
	 * Removes all subscribers and unregisters event bus change recorder form
	 * event bus.
	 */
	public void close() throws Exception {
		subscribers = new ConcurrentSet<>();
		registration.close();
		registration = null;
		eventBus.unregister(eventBusChangeRecorder);
	}

	/**
	 * Checks if {@link ListenerRegistration} registration exist.
	 * 
	 * @return True if exist, false otherwise.
	 */
	public boolean isListening() {
		return registration == null ? false : true;
	}

	/**
	 * Creates event of type {@link EventType#REGISTER}, set {@link Channel}
	 * subscriber to the event and post event into event bus.
	 * 
	 * @param subscriber
	 *            Channel
	 */
	public void addSubscriber(Channel subscriber) {
		if (!subscriber.isActive()) {
			logger.debug("Channel is not active between websocket server and subscriber {}"
					+ subscriber.remoteAddress());
		}
		Event event = new Event(EventType.REGISTER);
		event.setSubscriber(subscriber);
		eventBus.post(event);
	}

	/**
	 * Creates event of type {@link EventType#DEREGISTER}, sets {@link Channel}
	 * subscriber to the event and posts event into event bus.
	 * 
	 * @param subscriber
	 */
	public void removeSubscriber(Channel subscriber) {
		logger.debug("Subscriber {} is removed.", subscriber.remoteAddress());
		Event event = new Event(EventType.DEREGISTER);
		event.setSubscriber(subscriber);
		eventBus.post(event);
	}

	/**
	 * Checks if exists at least one {@link Channel} subscriber.
	 * 
	 * @return True if exist at least one {@link Channel} subscriber, false
	 *         otherwise.
	 */
	public boolean hasSubscribers() {
		return !subscribers.isEmpty();
	}

	/**
	 * Consists of two types {@link Store#CONFIG} and {@link Store#OPERATION}.
	 */
	private static enum Store {
		CONFIG("config"), OPERATION("operation");

		private final String value;

		private Store(String value) {
			this.value = value;
		}
	}

	/**
	 * Consists of three types {@link Operation#CREATED},
	 * {@link Operation#UPDATED} and {@link Operation#DELETED}.
	 */
	private static enum Operation {
		CREATED("created"), UPDATED("updated"), DELETED("deleted");

		private final String value;

		private Operation(String value) {
			this.value = value;
		}
	}

}
