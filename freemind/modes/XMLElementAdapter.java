/*FreeMind - A Program for creating and viewing Mindmaps
 *Copyright (C) 2000-2001  Joerg Mueller <joergmueller@bigfoot.com>
 *See COPYING for Details
 *
 *This program is free software; you can redistribute it and/or
 *modify it under the terms of the GNU General Public License
 *as published by the Free Software Foundation; either version 2
 *of the License, or (at your option) any later version.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License
 *along with this program; if not, write to the Free Software
 *Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package freemind.modes;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import freemind.extensions.PermanentNodeHook;
import freemind.extensions.PermanentNodeHookSubstituteUnknown;
import freemind.main.Tools;
import freemind.main.XMLElement;
import freemind.modes.attributes.Attribute;

public class XMLElementAdapter extends XMLElement {


	// Logging:
	protected static java.util.logging.Logger logger;

	private Object userObject = null;
	private NodeAdapter mapChild = null;
	private HashMap nodeAttributes = new HashMap();

	// Font attributes

	private String fontName;
	private int fontStyle = 0;
	private int fontSize = 0;

	// Icon attributes

	private String iconName;

	// arrow link attributes:
	protected Vector mArrowLinkAdapters;
	protected HashMap /* id -> target */mIdToTarget;
	public static final String XML_NODE_TEXT = "TEXT";
	public static final String XML_NODE = "node";
	public static final String XML_NODE_ATTRIBUTE = "attribute";
	public static final String XML_NODE_ATTRIBUTE_LAYOUT = "attribute_layout";
	public static final String XML_NODE_ATTRIBUTE_REGISTRY = "attribute_registry";
	public static final String XML_NODE_REGISTERED_ATTRIBUTE_NAME = "attribute_name";
	public static final String XML_NODE_REGISTERED_ATTRIBUTE_VALUE = "attribute_value";
	// public static final String XML_NODE_CLASS_PREFIX = XML_NODE+"_";
	public static final String XML_NODE_CLASS = "AA_NODE_CLASS";
	public static final String XML_NODE_ADDITIONAL_INFO = "ADDITIONAL_INFO";
	public static final String XML_NODE_HISTORY_CREATED_AT = "CREATED";
	public static final String XML_NODE_HISTORY_LAST_MODIFIED_AT = "MODIFIED";

	public static final String XML_NODE_XHTML_TYPE_TAG = "TYPE";
	public static final String XML_NODE_XHTML_TYPE_NODE = "NODE";
	public static final String XML_NODE_XHTML_TYPE_NOTE = "NOTE";

	private String attributeName;

	private String attributeValue;

	protected final MapFeedback mMapFeedback;

	private boolean fontStyleStrikethrough;

	// Overhead methods

	public XMLElementAdapter(MapFeedback pMapFeedback) {
		this(pMapFeedback, new Vector(), new HashMap());
	}

	protected XMLElementAdapter(MapFeedback pMapFeedback,
			Vector arrowLinkAdapters, HashMap IDToTarget) {
		this.mMapFeedback = pMapFeedback;
		this.mArrowLinkAdapters = arrowLinkAdapters;
		this.mIdToTarget = IDToTarget;
		if (logger == null) {
			logger = freemind.main.Resources.getInstance().getLogger(
					this.getClass().getName());
		}
	}

	/** abstract method to create elements of my type (factory). */
	protected XMLElement createAnotherElement() {
		// We do not need to initialize the things of XMLElement.
		return new XMLElementAdapter(mMapFeedback, mArrowLinkAdapters,
				mIdToTarget);
	}



	public Object getUserObject() {
		return userObject;
	}

	protected void setUserObject(Object obj) {
		userObject = obj;
	}

	public NodeAdapter getMapChild() {
		return mapChild;
	}

	// Real parsing methods

	public void setName(String name) {
		super.setName(name);
		// Create user object based on name
		switch (name) {
			case XML_NODE:
				userObject = getMap().createNodeAdapter(getMap(), null);
				nodeAttributes.clear();
				break;
			case "edge":
				userObject = getMap().createEdgeAdapter(null);
				break;
			case "cloud":
				userObject = getMap().createCloudAdapter(null);
				break;
			case "arrowlink":
				userObject = getMap().createArrowLinkAdapter(null, null);
				break;
			case "linktarget":
				userObject = getMap().createArrowLinkTarget(null, null);
				break;
			case "font":
				userObject = null;
				break;
			case XML_NODE_ATTRIBUTE:
				userObject = null;
				break;
			case XML_NODE_ATTRIBUTE_LAYOUT:
				userObject = null;
				break;
			case "map":
				userObject = null;
				break;
			case XML_NODE_ATTRIBUTE_REGISTRY:
				userObject = null;
				break;
			case XML_NODE_REGISTERED_ATTRIBUTE_NAME:
				userObject = null;
				break;
			case XML_NODE_REGISTERED_ATTRIBUTE_VALUE:
				userObject = null;
				break;
			case "icon":
				userObject = null;
				break;
			case "hook":
				// we gather the xml element and send it to the hook after
				// completion.
				userObject = new XMLElement();
				break;
			default:
				userObject = new XMLElement(); // for childs of hooks

				break;
		}
	}

	protected MindMap getMap() {
		return mMapFeedback.getMap();
	}

	public void addChild(XMLElement child) {
		if (getName().equals("map")) {
			mapChild = (NodeAdapter) child.getUserObject();
			return;
		}
		if (userObject instanceof XMLElement) {
			// ((XMLElement) userObject).addChild(child);
			super.addChild(child);
			return;
		}
		if (userObject instanceof NodeAdapter) {
			NodeAdapter node = (NodeAdapter) userObject;
			if (child.getUserObject() instanceof NodeAdapter) {
				node.insert((NodeAdapter) child.getUserObject(), -1);
			} // to the end without preferable... (PN)
				// node.getRealChildCount()); }
			else if (child.getUserObject() instanceof EdgeAdapter) {
				EdgeAdapter edge = (EdgeAdapter) child.getUserObject();
				edge.setTarget(node);
				node.setEdge(edge);
			} else if (child.getUserObject() instanceof CloudAdapter) {
				CloudAdapter cloud = (CloudAdapter) child.getUserObject();
				cloud.setTarget(node);
				node.setCloud(cloud);
			} else if (child.getUserObject() instanceof ArrowLinkTarget) {
				// ArrowLinkTarget is derived from ArrowLinkAdapter, so it must
				// be checked first.
				ArrowLinkTarget arrowLinkTarget = (ArrowLinkTarget) child
						.getUserObject();
				arrowLinkTarget.setTarget(node);
				mArrowLinkAdapters.add(arrowLinkTarget);
			} else if (child.getUserObject() instanceof ArrowLinkAdapter) {
				ArrowLinkAdapter arrowLink = (ArrowLinkAdapter) child
						.getUserObject();
				arrowLink.setSource(node);
				// annotate this link: (later processed by caller.).
				// System.out.println("arrowLink="+arrowLink);
				mArrowLinkAdapters.add(arrowLink);
			} else if (child.getName().equals("font")) {
				node.setFont((Font) child.getUserObject());
			} else if (child.getName().equals(XML_NODE_ATTRIBUTE)) {
				node.addAttribute(
						(Attribute) child.getUserObject());

			} else if (child.getName().equals(XML_NODE_ATTRIBUTE_LAYOUT)) {
//				node.createAttributeTableModel();
//				AttributeTableLayoutModel layout = node.getAttributes()
//						.getLayout();
//				layout.setColumnWidth(0,
//						((XMLElementAdapter) child).attributeNameWidth);
//				layout.setColumnWidth(1,
//						((XMLElementAdapter) child).attributeValueWidth);
			} else if (child.getName().equals("icon")) {
				node.addIcon((MindIcon) child.getUserObject(), MindIcon.LAST);
			} else if (child.getName().equals(XML_NODE_XHTML_CONTENT_TAG)) {
				String xmlText = ((XMLElement) child).getContent();
				Object typeAttribute = child
						.getAttribute(XML_NODE_XHTML_TYPE_TAG);
				if (typeAttribute == null
						|| XML_NODE_XHTML_TYPE_NODE.equals(typeAttribute)) {
					// output:
					logger.finest("Setting node html content to:" + xmlText);
					node.setXmlText(xmlText);
				} else {
					logger.finest("Setting note html content to:" + xmlText);
					node.setXmlNoteText(xmlText);
				}
			} else if (child.getName().equals("hook")) {
				XMLElement xml = (XMLElement) child/* .getUserObject() */;
				String loadName = (String) xml.getAttribute("NAME");
				PermanentNodeHook hook = null;
				try {
					// loadName=loadName.replace('/', File.separatorChar);
					/*
					 * The next code snippet is an exception. Normally, hooks
					 * have to be created via the ModeController. DO NOT COPY.
					 */
					hook = (PermanentNodeHook) mMapFeedback.createNodeHook(loadName, node);
					// this is a bad hack. Don't make use of this data unless
					// you know exactly what you are doing.
					hook.setNode(node);
				} catch (Exception e) {
					freemind.main.Resources.getInstance().logException(e);
					hook = new PermanentNodeHookSubstituteUnknown(loadName);
				}
				hook.loadFrom(xml);
				node.addHook(hook);
			}
			return;
		}
		if (child instanceof XMLElementAdapter
				&& getName().equals(XML_NODE_REGISTERED_ATTRIBUTE_NAME)
				&& child.getName().equals(XML_NODE_REGISTERED_ATTRIBUTE_VALUE)) {
			Attribute attribute = new Attribute(attributeName,
					((XMLElementAdapter) child).attributeValue);
		}
	}

	public void setAttribute(String name, Object value) {
		// We take advantage of precondition that value != null.
		String sValue = value.toString();
		if (ignoreCase) {
			name = name.toUpperCase();
		}
		if (userObject instanceof XMLElement) {
			// ((XMLElement) userObject).setAttribute(name, value);
			super.setAttribute(name, value); // and to myself, as I am also an
												// xml element.
			return;
		}

		if (userObject instanceof NodeAdapter) {
			//
			NodeAdapter node = (NodeAdapter) userObject;
			userObject = setNodeAttribute(name, sValue, node);
			nodeAttributes.put(name, sValue);
			return;
		}

		if (userObject instanceof EdgeAdapter) {
			EdgeAdapter edge = (EdgeAdapter) userObject;
			switch (name) {
				case "STYLE":
					edge.setStyle(sValue);
					break;
				case "COLOR":
					edge.setColor(Tools.xmlToColor(sValue));
					break;
				case "WIDTH":
					if (sValue.equals(EdgeAdapter.EDGE_WIDTH_THIN_STRING)) {
						edge.setWidth(EdgeAdapter.WIDTH_THIN);
					} else {
						edge.setWidth(Integer.parseInt(sValue));
					}
					break;
			}
			return;
		}

		if (userObject instanceof CloudAdapter) {
			CloudAdapter cloud = (CloudAdapter) userObject;
			switch (name) {
				case "STYLE":
					cloud.setStyle(sValue);
					break;
				case "COLOR":
					cloud.setColor(Tools.xmlToColor(sValue));
					break;
				case "WIDTH":
					cloud.setWidth(Integer.parseInt(sValue));
					break;
			}
			return;
		}

		if (userObject instanceof ArrowLinkAdapter) {
			ArrowLinkAdapter arrowLink = (ArrowLinkAdapter) userObject;
			switch (name) {
				case "STYLE":
					arrowLink.setStyle(sValue);
					break;
				case "ID":
					arrowLink.setUniqueId(sValue);
					break;
				case "COLOR":
					arrowLink.setColor(Tools.xmlToColor(sValue));
					break;
				case "DESTINATION":
					arrowLink.setDestinationLabel(sValue);
					break;
				case "REFERENCETEXT":
					arrowLink.setReferenceText((sValue));
					break;
				case "STARTINCLINATION":
					arrowLink.setStartInclination(Tools.xmlToPoint(sValue));
					break;
				case "ENDINCLINATION":
					arrowLink.setEndInclination(Tools.xmlToPoint(sValue));
					break;
				case "STARTARROW":
					arrowLink.setStartArrow(sValue);
					break;
				case "ENDARROW":
					arrowLink.setEndArrow(sValue);
					break;
				case "WIDTH":
					arrowLink.setWidth(Integer.parseInt(sValue));
					break;
			}
			if (userObject instanceof ArrowLinkTarget) {
				ArrowLinkTarget arrowLinkTarget = (ArrowLinkTarget) userObject;
				if (name.equals("SOURCE")) {
					arrowLinkTarget.setSourceLabel(sValue);
				}
			}
			return;
		}

		if (getName().equals("font")) {
			if (name.equals("SIZE")) {
				fontSize = Integer.parseInt(sValue);
			} else if (name.equals("NAME")) {
				fontName = sValue;
			}

			// Styling
			else if (sValue.equals("true")) {
				switch (name) {
					case "BOLD":
						fontStyle += Font.BOLD;
						break;
					case "ITALIC":
						fontStyle += Font.ITALIC;
						break;
					case "STRIKETHROUGH":
						fontStyleStrikethrough = true;
						break;
				}
			}
		}
		/* icons */
		switch (getName()) {
			case "icon":
				if (name.equals("BUILTIN")) {
					iconName = sValue;
				}
				break;
		/* attributes */
			case XML_NODE_ATTRIBUTE:
				if (name.equals("NAME")) {
					attributeName = sValue;
				} else if (name.equals("VALUE")) {
					attributeValue = sValue;
				}
				break;
			case XML_NODE_ATTRIBUTE_LAYOUT:
//			if (name.equals("NAME_WIDTH")) {
//				attributeNameWidth = Integer.parseInt(sValue);
//			} else if (name.equals("VALUE_WIDTH")) {
//				attributeValueWidth = Integer.parseInt(sValue);
//			}
				break;
			case XML_NODE_ATTRIBUTE_REGISTRY:
				break;
			case XML_NODE_REGISTERED_ATTRIBUTE_NAME:
				if (name.equals("NAME")) {
					attributeName = sValue;
				} else {
					super.setAttribute(name, sValue);
				}
				break;
			case XML_NODE_REGISTERED_ATTRIBUTE_VALUE:
				if (name.equals("VALUE")) {
					attributeValue = sValue;
				}
				break;
		}
	}

	private NodeAdapter setNodeAttribute(String name, String sValue,
			NodeAdapter node) {
		switch (name) {
			case XML_NODE_TEXT:
				logger.finest("Setting node text content to:" + sValue);
				node.setUserObject(sValue);
				break;
			case XML_NODE_HISTORY_CREATED_AT:
				if (node.getHistoryInformation() == null) {
					node.setHistoryInformation(new HistoryInformation());
				}
				node.getHistoryInformation().setCreatedAt(Tools.xmlToDate(sValue));
				break;
			case XML_NODE_HISTORY_LAST_MODIFIED_AT:
				if (node.getHistoryInformation() == null) {
					node.setHistoryInformation(new HistoryInformation());
				}
				node.getHistoryInformation().setLastModifiedAt(
						Tools.xmlToDate(sValue));
				break;
			case "FOLDED":
				if (sValue.equals("true")) {
					node.setFolded(true);
				}
				break;
			case "POSITION":
				// fc, 17.12.2003: Remove the left/right bug.
				node.setLeft(sValue.equals("left"));
				break;
			case "COLOR":
				if (sValue.length() == 7) {
					node.setColor(Tools.xmlToColor(sValue));
				}
				break;
			case "BACKGROUND_COLOR":
				if (sValue.length() == 7) {
					node.setBackgroundColor(Tools.xmlToColor(sValue));
				}
				break;
			case "LINK":
				node.setLink(sValue);
				break;
			case "STYLE":
				node.setStyle(sValue);
				break;
			case "ID":
				// do not set label but annotate in list:
				// System.out.println("(sValue, node) = " + sValue + ", "+ node);
				mIdToTarget.put(sValue, node);
				break;
			case "VSHIFT":
				node.setShiftY(Integer.parseInt(sValue));
				break;
			case "VGAP":
				node.setVGap(Integer.parseInt(sValue));
				break;
			case "HGAP":
				node.setHGap(Integer.parseInt(sValue));
				break;
		}
		return node;
	}

	/**
	 * Sets all attributes that were formely applied to the current userObject
	 * to a given (new) node. Thus, the instance of a node can be changed after
	 * the creation. (At the moment, relevant for encrypted nodes).
	 */
	protected void copyAttributesToNode(NodeAdapter node) {
		// reactivate all settings from nodeAttributes:
		for (Object o : nodeAttributes.keySet()) {
			String key = (String) o;
			// to avoid self reference:
			setNodeAttribute(key, (String) nodeAttributes.get(key), node);
		}
	}

	protected void completeElement() {
		if (getName().equals(XML_NODE)) {
			// unify map child behaviour:
			if (mapChild == null) {
				mapChild = (NodeAdapter) userObject;
			}
			return;
		}
		if (getName().equals("font")) {
			Font font = new Font(fontName, fontStyle, fontSize);
			if(fontStyleStrikethrough){
				Map attr = font.getAttributes();
				attr.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
				font = new Font(attr);
			}
			userObject = mMapFeedback.getFontThroughMap(font);
			return;
		}
		/* icons */
		if (getName().equals("icon")) {
			userObject = MindIcon.factory(iconName);
			return;
		}
		/* attributes */
		if (getName().equals(XML_NODE_ATTRIBUTE)) {
			userObject = new Attribute(attributeName, attributeValue);
			return;
		}
		if (getName().equals(XML_NODE_REGISTERED_ATTRIBUTE_NAME)) {
		}
	}

	/**
	 * Completes the links within the getMap(). They are registered in the
	 * registry.
	 * 
	 * Case I: Source+Destination are pasted (Ia: cut, Ib: copy) Case II: Source
	 * is pasted, Destination remains unchanged in the map (IIa: cut, IIb: copy)
	 * Case III: Destination is pasted, Source remains unchanged in the map
	 * (IIIa: cut, IIIb: copy)
	 */
	public void processUnfinishedLinks(MindMapLinkRegistry registry) {
		// add labels to the nodes:
		for (Object o : mIdToTarget.keySet()) {
			String key = (String) o;
			NodeAdapter target1 = (NodeAdapter) mIdToTarget.get(key);
			/*
			 * key is the proposed name for the target, is changed by the
			 * registry, if already present.
			 */
			registry.registerLinkTarget(target1, key);
		}
		// complete arrow links with right labels:
		for (Object arrowObject : mArrowLinkAdapters) {
			if (arrowObject instanceof ArrowLinkTarget) {
				ArrowLinkTarget linkTarget = (ArrowLinkTarget) arrowObject;
				// do the same as for ArrowLinkAdapter and start to search for the source.
				String oldId = linkTarget.getSourceLabel();
				MindMapNode source = (MindMapNode) registry.getTargetForId(oldId);
				// find oldId in target list:
				if (mIdToTarget.containsKey(oldId)) {
					// link source present in the paste as well and has probably
					// been renamed (case I), do nothing, as the source does
					// all.
					continue;
				} else if (source == null) {
					// link source is in nowhere-land
					logger.severe("Cannot find the label " + oldId
							+ " in the map. The link target " + linkTarget
							+ " is not restored.");
					continue;
				}
				// link source remains in the map (case III)
				// set the source:
				linkTarget.setSource(source);
				// here, it is getting more complex: case IIIa: the arrowLink
				// has to be recreated.
				// case IIIb: the arrowLink has to be doubled! First,
				// distinguish between a and b.
				MindMapNode target = linkTarget.getTarget();
				String targetCurrentId = registry.getLabel(target);
				if (!mIdToTarget.containsKey(targetCurrentId)) {
					// the id of target has changed, we have case IIIb.
					MindMapLink link = registry.getLinkForId(linkTarget
							.getUniqueId());
					if (link == null) {
						logger.severe("Cannot find the label "
								+ linkTarget.getUniqueId()
								+ " for the link in the map. The link target "
								+ linkTarget + " is not restored.");
					} else {
						// double the link.
						MindMapLink clone = (MindMapLink) link.clone();
						clone.setTarget(target);
						((ArrowLinkAdapter) clone)
								.setDestinationLabel(targetCurrentId);
						// add the new arrowLink and give it a new id
						registry.registerLink(clone);
						linkTarget.setUniqueId(clone.getUniqueId());
					}
				} else {
					// case IIIa: The link must only be re-added:
					// change from linkTarget to ArrowLinkAdapter and add it:
					ArrowLinkAdapter linkAdapter = linkTarget.createArrowLinkAdapter(registry);
					registry.registerLink(linkAdapter);
				}
			} else if (arrowObject instanceof ArrowLinkAdapter) {
				ArrowLinkAdapter arrowLink = (ArrowLinkAdapter) arrowObject;
				// here, the source is in the paste, and the destination is now
				// searched:
				String oldId = arrowLink.getDestinationLabel();
				NodeAdapter target = null;
				String newId = null;
				// find oldId in target list:
				if (mIdToTarget.containsKey(oldId)) {
					// link target present in the paste as well and has probably
					// been renamed (case I)
					target = (NodeAdapter) mIdToTarget.get(oldId);
					newId = registry.getLabel(target);
				} else if (registry.getTargetForId(oldId) != null) {
					// link target remains in the map (case II)
					target = (NodeAdapter) registry.getTargetForId(oldId);
					newId = oldId;
				} else {
					// link target is in nowhere-land
					logger.severe("Cannot find the label " + oldId
							+ " in the map. The link " + arrowLink
							+ " is not restored.");
					continue;
				}
				// set the new ID:
				arrowLink.setDestinationLabel(newId);
				// set the target:
				arrowLink.setTarget(target);
				// add the arrowLink:
				registry.registerLink(arrowLink);
			}
		}
	}

	public HashMap getIDToTarget() {
		return mIdToTarget;
	}

	public void setIDToTarget(HashMap pToTarget) {
		mIdToTarget = pToTarget;
	}


}
