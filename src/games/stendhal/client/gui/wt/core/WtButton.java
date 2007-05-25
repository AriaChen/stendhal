/* $Id$ */
/***************************************************************************
 *                      (C) Copyright 2005 - Marauroa                      *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
/*
 * Button.java
 *
 * Created on 23. Oktober 2005, 09:47
 */

package games.stendhal.client.gui.wt.core;

import games.stendhal.client.Sprite;

import java.awt.Graphics;
import java.awt.Point;

/**
 * A simple button. It features a onClick() callback. A button can have an image
 * <b>or</b> some text, but not both.
 * 
 * @author mtotz
 */
public class WtButton extends WtPanel {

	/** image for the button */
	private Sprite image;

	/** Creates a new Button with text */
	public WtButton(String name, int width, int height, String text) {
		super(name, 0, 0, width, height);
		initialize();
		int clientHeight = (getClientHeight() - WtTextPanel.DEFAULT_FONT_SIZE) / 2;
		WtTextPanel textPanel = new WtTextPanel(name + "text", 2, clientHeight, width, height, text);
		addChild(textPanel);
		setTitletext(text);
	}

	/** Creates a new Button with an image */
	public WtButton(String name, int width, int height, Sprite image) {
		super(name, 0, 0, width, height);
		initialize();
		this.image = image;
	}

	/** some initialisations */
	private void initialize() {
		setMinimizeable(false);
		setTitleBar(false);
		setFrame(true);

	}

	/** draws the button */
	@Override
	public Graphics draw(Graphics g) {
		if (isClosed()) {
			return g;
		}

		Graphics clientArea = super.draw(g);

		// draw the image if we have one
		if (image != null) {
			image.draw(clientArea, 0, 0);
		}

		return clientArea;
	}

	/** button is clicked */
	@Override
	public boolean onMouseClick(Point point) {
		setEmboss(!isEmbossed());
		// tell all registered listeners that we're clicked
		notifyClickListeners(getName(), point);
		return true;
	}

	/** returns true when the button in pressed and false when it is released */
	public boolean isPressed() {
		return isEmbossed();
	}

	/** sets the state of the button */
	public void setPressed(boolean pressed) {
		setEmboss(pressed);
	}
}
