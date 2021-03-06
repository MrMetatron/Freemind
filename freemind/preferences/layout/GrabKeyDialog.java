/*
 * GrabKeyDialog.java - Grabs keys from the keyboard
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2002 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package freemind.preferences.layout;

import java.awt.AWTEvent;
import java.awt.Dialog;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import freemind.main.FreeMindMain;
import freemind.main.Resources;

public class GrabKeyDialog extends JDialog {
	private final FreeMindMain fmMain;

	private static class Buffer {

		public int getLength() {
			return 0;
		}

		public void insert(int length, String string) {
		}

	}

	public static String toString(KeyEvent evt) {
		String id;
		switch (evt.getID()) {
		case KeyEvent.KEY_PRESSED:
			id = "KEY_PRESSED";
			break;
		case KeyEvent.KEY_RELEASED:
			id = "KEY_RELEASED";
			break;
		case KeyEvent.KEY_TYPED:
			id = "KEY_TYPED";
			break;
		default:
			id = "unknown type";
			break;
		}

		return id + ",keyCode=0x" + Integer.toString(evt.getKeyCode(), 16)
				+ ",keyChar=0x" + Integer.toString(evt.getKeyChar(), 16)
				+ ",modifiers=0x" + Integer.toString(evt.getModifiers(), 16);
	}

	public GrabKeyDialog(FreeMindMain fmMain, Dialog parent,
			KeyBinding binding, Vector<KeyBinding> allBindings, Buffer debugBuffer) {
		this(fmMain, parent, binding, allBindings, debugBuffer, 0);
	}

	public GrabKeyDialog(FreeMindMain fmMain, Dialog parent,
			KeyBinding binding, Vector<KeyBinding> allBindings, Buffer debugBuffer,
			int modifierMask) {
		super(parent, (("grab-key.title")), true);
		this.fmMain = fmMain;
		this.modifierMask = modifierMask;
		setTitle(getText("grab-key.title"));

		init(binding, allBindings, debugBuffer);
	}

	public String getShortcut() {
		if (isOK)
			return shortcut.getText();
		else
			return null;
	}

	public boolean isOK() {
		return isOK;
	} // }}}

	public boolean isManagingFocus() {
		return false;
	} // }}}

	public boolean getFocusTraversalKeysEnabled() {
		return false;
	} // }}}

	protected void processKeyEvent(KeyEvent evt) {
		shortcut.processKeyEvent(evt);
	} // }}}

	private InputPane shortcut;
	private JLabel assignedTo;
	private JButton ok;
	private JButton remove;
	private JButton cancel;
	private JButton clear;
	private boolean isOK;
	private KeyBinding binding;
	KeyBinding bindingReset;
	private Vector<KeyBinding> allBindings;
	private Buffer debugBuffer;
	private int modifierMask;
	public final static String MODIFIER_SEPARATOR = " ";

	private void init(KeyBinding binding, Vector<KeyBinding> allBindings, Buffer debugBuffer) {
		this.binding = binding;
		this.allBindings = allBindings;
		this.debugBuffer = debugBuffer;

		enableEvents(AWTEvent.KEY_EVENT_MASK);

		JPanel content = new JPanel(new GridLayout(0, 1, 0, 6));
		content.setBorder(new EmptyBorder(12, 12, 12, 12));
		setContentPane(content);

		Box input = Box.createHorizontalBox();

		shortcut = new InputPane();
		input.add(shortcut);
		input.add(Box.createHorizontalStrut(12));

		clear = new JButton((getText("grab-key.clear")));
		clear.addActionListener(new ActionHandler());
		input.add(clear);

		assignedTo = new JLabel();
		if (debugBuffer == null)
			updateAssignedTo(null);

		Box buttons = Box.createHorizontalBox();
		buttons.add(Box.createGlue());

		if (debugBuffer == null) {
			ok = new JButton(getText("common.ok"));
			ok.addActionListener(new ActionHandler());
			buttons.add(ok);
			buttons.add(Box.createHorizontalStrut(12));

			if (binding.isAssigned()) {
				remove = new JButton((getText("grab-key.remove")));
				remove.addActionListener(new ActionHandler());
				buttons.add(Box.createHorizontalStrut(12));
			}
		}

		cancel = new JButton(getText("common.cancel"));
		cancel.addActionListener(new ActionHandler());
		buttons.add(cancel);
		buttons.add(Box.createGlue());

		content.add(input);
		content.add(buttons);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		pack();
		setLocationRelativeTo(getParent());
		setResizable(false);
		setVisible(true);
	}

	private String getSymbolicName(int keyCode) {
		if (keyCode == KeyEvent.VK_UNDEFINED)
			return null;

		if (keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
			return String.valueOf(Character.toLowerCase((char) keyCode));
		}

		try {
			Field[] fields = KeyEvent.class.getFields();
			for (Field field : fields) {
				String name = field.getName();
				if (name.startsWith("VK_") && field.getInt(null) == keyCode) {
					return name.substring(3);
				}
			}
		} catch (Exception e) {
			// Log.log(Log.ERROR,this,e);
		}

		return null;
	}

	private void updateAssignedTo(String shortcut) {
		String text = (getText("grab-key.assigned-to.none"));
		KeyBinding kb = getKeyBinding(shortcut);

		if (kb != null)
			if (kb.isPrefix) {
                text = getText("grab-key.assigned-to.prefix") + " " + shortcut;
            } else {
                text = kb.label;
            }

		if (ok != null)
			ok.setEnabled(kb == null || !kb.isPrefix);

		assignedTo.setText((getText("grab-key.assigned-to") + " " + text));
	}

	private KeyBinding getKeyBinding(String shortcut) {
		if (shortcut == null || shortcut.length() == 0)
			return null;

		String spacedShortcut = shortcut + " ";
		Enumeration<KeyBinding> e = allBindings.elements();

		while (e.hasMoreElements()) {
			KeyBinding kb = e.nextElement();

			if (!kb.isAssigned())
				continue;

			String spacedKbShortcut = kb.shortcut + " ";

			if (spacedShortcut.startsWith(spacedKbShortcut))
				return kb;

			if (spacedKbShortcut.startsWith(spacedShortcut)) {
				return new KeyBinding(kb.name, kb.label, shortcut, true);
			}
		}

		return null;
	}

	public static class KeyBinding {
		public KeyBinding(String name, String label, String shortcut,
				boolean isPrefix) {
			this.name = name;
			this.label = label;
			this.shortcut = shortcut;
			this.isPrefix = isPrefix;
		}

		public String name;
		public String label;
		public String shortcut;
		public boolean isPrefix;

		public boolean isAssigned() {
			return shortcut != null && shortcut.length() > 0;
		}
	}

	class InputPane extends JTextField {
		public boolean getFocusTraversalKeysEnabled() {
			return false;
		} // }}}

		protected void processKeyEvent(KeyEvent _evt) {
			if ((getModifierMask() & _evt.getModifiers()) != 0) {
				KeyEvent evt = new KeyEvent(_evt.getComponent(), _evt.getID(),
						_evt.getWhen(), ~getModifierMask()
								& _evt.getModifiers(), _evt.getKeyCode(),
						_evt.getKeyChar(), _evt.getKeyLocation());
				processKeyEvent(evt);
				if (evt.isConsumed()) {
					_evt.consume();
				}
				return;
			}
			KeyEvent evt = KeyEventWorkaround.processKeyEvent(_evt);
			if (debugBuffer != null) {
				debugBuffer.insert(debugBuffer.getLength(), "Event "
						+ GrabKeyDialog.toString(_evt)
						+ (evt == null ? " filtered\n" : " passed\n"));
			}

			if (evt == null)
				return;

			evt.consume();

			KeyEventTranslator.Key key = KeyEventTranslator
					.translateKeyEvent(evt);
			if (key == null)
				return;

			if (debugBuffer != null) {
				debugBuffer.insert(debugBuffer.getLength(), "==> Translated to " + key + "\n");
			}

			StringBuilder keyString = new StringBuilder();

			if (key.modifiers != null)
				keyString.append(key.modifiers).append(' ');

			if (key.input == ' ')
				keyString.append("SPACE");
			else if (key.input != '\0')
				keyString.append(key.input);
			else {
				String symbolicName = getSymbolicName(key.key);

				if (symbolicName == null)
					return;

				keyString.append(symbolicName);
			}

			setText(keyString.toString());
			if (debugBuffer == null)
				updateAssignedTo(keyString.toString());
		}
	}

	class ActionHandler implements ActionListener {
		public void actionPerformed(ActionEvent evt) {
			if (evt.getSource() == ok) {
				if (canClose())
					dispose();
			} else if (evt.getSource() == remove) {
				shortcut.setText(null);
				isOK = true;
				dispose();
			} else if (evt.getSource() == cancel)
				dispose();
			else if (evt.getSource() == clear) {
				shortcut.setText(null);
				if (debugBuffer == null)
					updateAssignedTo(null);
				shortcut.requestFocus();
			}
		}

		private boolean canClose() {
			String shortcutString = shortcut.getText();
			if (shortcutString.length() == 0 && binding.isAssigned()) {
				int answer = JOptionPane
						.showConfirmDialog(GrabKeyDialog.this,
								getText("grab-key.remove-ask"), null,
								JOptionPane.YES_NO_OPTION,
								JOptionPane.QUESTION_MESSAGE);
				if (answer == JOptionPane.YES_OPTION) {
					shortcut.setText(null);
					isOK = true;
				} else
					return false;
			}

			bindingReset = getKeyBinding(shortcutString);
			
			if (bindingReset == null || bindingReset == binding) {
				isOK = true;
				return true;
			}

			if (Objects.equals(bindingReset.name, binding.name)) {
				JOptionPane.showMessageDialog(GrabKeyDialog.this, getText("grab-key.duplicate-alt-shortcut"));
				return false;
			}

			if (bindingReset.isPrefix) {
				JOptionPane.showMessageDialog(GrabKeyDialog.this, getText("grab-key.prefix-shortcut"));
				return false;
			}

			int answer = JOptionPane.showConfirmDialog(GrabKeyDialog.this,
					Resources.getInstance().format(
							"GrabKeyDialog.grab-key.duplicate-shortcut",
							new Object[] { bindingReset.name }), null, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (answer == JOptionPane.YES_OPTION) {
				if (bindingReset.shortcut != null && shortcutString.startsWith(bindingReset.shortcut)) {
					bindingReset.shortcut = null;
				}
				isOK = true;
				return true;
			} else {
                return false;
            }
		}

	}

	private String getText(String resourceString) {
		return fmMain.getResourceString("GrabKeyDialog." + resourceString);
	}

	private int getModifierMask() {
		return modifierMask;
	}
}
