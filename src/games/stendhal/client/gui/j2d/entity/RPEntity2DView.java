/* $Id$ */
/***************************************************************************
 *                   (C) Copyright 2003-2010 - Stendhal                    *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.client.gui.j2d.entity;


import games.stendhal.client.IGameScreen;
import games.stendhal.client.entity.ActionType;
import games.stendhal.client.entity.Entity;
import games.stendhal.client.entity.IEntity;
import games.stendhal.client.entity.Player;
import games.stendhal.client.entity.RPEntity;
import games.stendhal.client.entity.TextIndicator;
import games.stendhal.client.entity.User;
import games.stendhal.client.gui.j2d.entity.helpers.AttackPainter;
import games.stendhal.client.gui.j2d.entity.helpers.HorizontalAlignment;
import games.stendhal.client.gui.j2d.entity.helpers.VerticalAlignment;
import games.stendhal.client.sprite.AnimatedSprite;
import games.stendhal.client.sprite.Sprite;
import games.stendhal.client.sprite.SpriteStore;
import games.stendhal.client.sprite.TextSprite;
import games.stendhal.common.Debug;
import games.stendhal.common.Direction;
import games.stendhal.common.constants.Nature;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import marauroa.common.game.RPAction;

/**
 * The 2D view of an RP entity.
 * 
 * @param <T> type of RPEntity
 */
abstract class RPEntity2DView<T extends RPEntity> extends ActiveEntity2DView<T> {
	private static final int ICON_OFFSET = 8;
	private static final int HEALTH_BAR_HEIGHT = 4;

	private static final Sprite eatingSprite;
	private static final Sprite poisonedSprite;
	private static final Sprite chokingSprite;
	private static final Sprite hitSprite;
	private static final Sprite blockedSprite;
	private static final Sprite missedSprite;
	
	/** Colors of the ring/circle around the player while attacking or being attacked. */
	private static final Color RING_COLOR_RED = new Color(230, 10, 10);
	private static final Color RING_COLOR_DARK_RED = new Color(74, 0, 0);
	private static final Color RING_COLOR_ORANGE = new Color(255, 200, 0);
	
	private static final double SQRT2 = 1.414213562;
	
	/** Temporary text sprites, like HP and XP changes. */
	private Map<TextIndicator, Sprite> floaters = new HashMap<TextIndicator, Sprite>();

	/**
	 * Model attributes effecting the title changed.
	 */
	private boolean titleChanged;
	/** <code>true</code> if the view should show the entity title. */
	private boolean showTitle;
	/** <code>true</code> if the view should show the HP bar. */
	private boolean showHP;

	/**
	 * The title image sprite.
	 */
	private Sprite titleSprite;

	/*
	 * The drawn height.
	 */
	protected int height;

	/*
	 * The drawn width.
	 */
	protected int width;
	/** Status icon managers. */
	private final List<StatusIconManager> iconManagers = new ArrayList<StatusIconManager>();
	private HealthBar healthBar; 

	/**
	 * Flag for checking if the entity is attacking. Can be modified by both
	 * the EDT and the game loop.
	 */
	private volatile boolean isAttacking;
	/** <code>true</code> if the current attack is ranged. */
	private boolean rangedAttack;

	/** Object for drawing the attack. */
	private AttackPainter attackPainter;
	
	static {
		final SpriteStore st = SpriteStore.get();

		hitSprite = st.getSprite("data/sprites/combat/hitted.png");
		blockedSprite = st.getSprite("data/sprites/combat/blocked.png");
		missedSprite = st.getSprite("data/sprites/combat/missed.png");
		eatingSprite = st.getSprite("data/sprites/ideas/eat.png");
		poisonedSprite = st.getAnimatedSprite(st.getSprite("data/sprites/status/poison.png"), 100);
		chokingSprite = st.getSprite("data/sprites/ideas/choking.png");
	}

	/**
	 * Create a new RPEntity2DView.
	 */
	public RPEntity2DView() {
		addIconManager(new StatusIconManager(Player.PROP_EATING, eatingSprite,
				HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM, 0, 0) {
					@Override
					boolean show(T rpentity) {
						return rpentity.isEating() && !rpentity.isChoking();
					}
				});
		addIconManager(new StatusIconManager(Player.PROP_EATING, chokingSprite,
				HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM, 0, 0) {
					@Override
					boolean show(T rpentity) {
						return rpentity.isChoking();
					}
				});
		addIconManager(new StatusIconManager(Player.PROP_POISONED, poisonedSprite,
				HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE, 10, -13) {
					@Override
					boolean show(T rpentity) {
						return rpentity.isPoisoned();
					}
				});
		setSpriteAlignment(HorizontalAlignment.CENTER, VerticalAlignment.BOTTOM);
	}

	@Override
	public void initialize(final T entity) {
		super.initialize(entity);
		showTitle = entity.showTitle();
		showHP = entity.showHPBar();
		if (showTitle) {
			titleSprite = createTitleSprite();
		}
		titleChanged = false;
	}
	//
	// RPEntity2DView
	//

	/**
	 * Populate keyed state sprites.
	 * 
	 * @param map
	 *            The map to populate.
	 * @param tiles
	 *            The master sprite.
	 * @param width
	 *            The tile width (in pixels).
	 * @param height
	 *            The tile height (in pixels).
	 */
	protected void buildSprites(final Map<Object, Sprite> map,
			final Sprite tiles, final int width, final int height) {
		int y = 0;
		map.put(Direction.UP, createWalkSprite(tiles, y, width, height));

		y += height;
		map.put(Direction.RIGHT, createWalkSprite(tiles, y, width, height));

		y += height;
		map.put(Direction.DOWN, createWalkSprite(tiles, y, width, height));

		y += height;
		map.put(Direction.LEFT, createWalkSprite(tiles, y, width, height));
	}

	/**
	 * Create the title sprite.
	 * 
	 * @return The title sprite.
	 */
	private Sprite createTitleSprite() {
		final String titleType = entity.getTitleType();
		final int adminlevel = entity.getAdminLevel();
		Color nameColor = null;

		if (titleType != null) {
			if (titleType.equals("npc")) {
				nameColor = new Color(200, 200, 255);
			} else if (titleType.equals("enemy")) {
				nameColor = new Color(255, 200, 200);
			}
		}

		if (nameColor == null) {
			if (adminlevel >= 800) {
				nameColor = new Color(200, 200, 0);
			} else if (adminlevel >= 400) {
				nameColor = Color.yellow;
			} else if (adminlevel > 0) {
				nameColor = new Color(255, 255, 172);
			} else {
				nameColor = Color.white;
			}
		}

		return TextSprite.createTextSprite(entity.getTitle(), nameColor);
	}

	/**
	 * Extract a walking animation for a specific row. The source sprite
	 * contains 3 animation tiles, but this is converted to 4 frames.
	 * 
	 * @param tiles
	 *            The tile image.
	 * @param y
	 *            The base Y coordinate.
	 * @param width
	 *            The frame width.
	 * @param height
	 *            The frame height.
	 * 
	 * @return A sprite.
	 */
	protected Sprite createWalkSprite(final Sprite tiles, final int y,
			final int width, final int height) {
		final SpriteStore store = SpriteStore.get();

		final Sprite[] frames = new Sprite[4];

		int x = 0;
		frames[0] = store.getTile(tiles, x, y, width, height);

		x += width;
		frames[1] = store.getTile(tiles, x, y, width, height);

		x += width;
		frames[2] = store.getTile(tiles, x, y, width, height);

		frames[3] = frames[1];

		return new AnimatedSprite(frames, 100, false);
	}
	
	/**
	 * Add a new status icon manager.
	 * 
	 * @param manager
	 */
	void addIconManager(StatusIconManager manager) {
		iconManagers.add(manager);
	}

	/**
	 * Draw the floating text indicators (floaters).
	 * 
	 * @param g2d
	 *            The graphics context.
	 * @param x
	 *            The drawn X coordinate.
	 * @param y
	 *            The drawn Y coordinate.
	 * @param width
	 *            The drawn width.
	 */
	private void drawFloaters(final Graphics2D g2d, final int x, final int y,
			final int width) {
		for (Map.Entry<TextIndicator, Sprite> floater : floaters.entrySet()) {
			final TextIndicator indicator = floater.getKey();
			final Sprite sprite = floater.getValue();
			final int age = indicator.getAge();
			
			final int tx = x + (width - sprite.getWidth()) / 2;
			final int ty = y - (int) (age * 5L / 300L);
			sprite.draw(g2d, tx, ty);
		}
	}

	/**
	 * Draw the entity HP bar.
	 * 
	 * @param g2d
	 *            The graphics context.
	 * @param x
	 *            The drawn X coordinate.
	 * @param y
	 *            The drawn Y coordinate.
	 * @param width
	 *            The drawn width.
	 */
	protected void drawHPbar(final Graphics2D g2d, final int x, final int y,
			final int width) {
		int dx = (width - healthBar.getWidth()) / 2;
		healthBar.draw(g2d, x + dx, y - healthBar.getHeight());
	}

	/**
	 * Draw the entity status bar. The status bar show the title and HP bar.
	 * 
	 * @param g2d
	 *            The graphics context.
	 * @param x
	 *            The drawn X coordinate.
	 * @param y
	 *            The drawn Y coordinate.
	 * @param width
	 *            The drawn width.
	 */
	protected void drawStatusBar(final Graphics2D g2d, final int x,
			final int y, final int width) {
	    if (showTitle) {
	        drawTitle(g2d, x, y, width);
	    }
		if (showHP) {
		    drawHPbar(g2d, x, y, width);
		}
	}

	/**
	 * Draw the entity title.
	 * 
	 * @param g2d
	 *            The graphics context.
	 * @param x
	 *            The drawn X coordinate.
	 * @param y
	 *            The drawn Y coordinate.
	 * @param width
	 *            The drawn width.
	 */
	protected void drawTitle(final Graphics2D g2d, final int x, final int y, final int width) {
		if (titleSprite != null) {
			final int tx = x + ((width - titleSprite.getWidth()) / 2);
			final int ty = y - 3 - titleSprite.getHeight();

			titleSprite.draw(g2d, tx, ty);
		}
	}

	/**
	 * Draw the combat indicators. 
	 * 
	 * @param g2d
	 *            The graphics context.
	 * @param x
	 *            The drawn X coordinate.
	 * @param y
	 *            The drawn Y coordinate.
	 * @param width
	 *            The drawn entity width.
	 * @param height
	 *            The drawn entity height.
	 */
	private void drawCombat(final Graphics2D g2d, final int x,
							  final int y, final int width, final int height) {
		Rectangle2D wrect = entity.getArea();
		final Rectangle srect = new Rectangle(
				(int) (wrect.getX() * IGameScreen.SIZE_UNIT_PIXELS),
				(int) (wrect.getY() * IGameScreen.SIZE_UNIT_PIXELS), 
				(int) (wrect.getWidth() * IGameScreen.SIZE_UNIT_PIXELS),
				(int) (wrect.getHeight() * IGameScreen.SIZE_UNIT_PIXELS)
		);
		
		// Calculating the circle's height
		int circleHeight = (int) ((srect.height - 2) / SQRT2);
		circleHeight = Math.max(circleHeight, srect.height - IGameScreen.SIZE_UNIT_PIXELS / 2);
		
		// When the entity is attacking the user give him a orange ring
		if (entity.isAttacking(User.get())) {
			g2d.setColor(RING_COLOR_ORANGE); 
			g2d.drawOval(srect.x - 1, srect.y + srect.height - circleHeight, srect.width, circleHeight);
			g2d.drawOval(srect.x, srect.y + srect.height - circleHeight, srect.width, circleHeight);
			g2d.drawOval(srect.x + 1, srect.y + srect.height - circleHeight, srect.width, circleHeight);
			drawShadedOval(g2d, srect.x + 1, srect.y + srect.height - circleHeight + 1, srect.width - 2, circleHeight - 2, RING_COLOR_ORANGE, true, false);
		}
		
		// When the entity is attacked by another entity
		if (entity.isBeingAttacked()) {
			Color lineColor;
			g2d.setColor(RING_COLOR_RED);
			
			// When it is also attacking the user give him only a red outline
			if (entity.isAttacking(User.get())) {
				lineColor = RING_COLOR_RED;
				drawShadedOval(g2d, srect.x - 1, srect.y + srect.height - circleHeight - 1, srect.width + 2, circleHeight + 2, RING_COLOR_RED, false, true);
			} else {
				// Otherwise make his complete ring red
				lineColor = RING_COLOR_DARK_RED;
				g2d.drawOval(srect.x - 1, srect.y + srect.height - circleHeight, srect.width, circleHeight);
				g2d.drawOval(srect.x, srect.y + srect.height - circleHeight, srect.width, circleHeight);
				g2d.drawOval(srect.x + 1, srect.y + srect.height - circleHeight, srect.width, circleHeight);
				drawShadedOval(g2d, srect.x + 1, srect.y + srect.height - circleHeight + 1, srect.width - 2, circleHeight - 2, RING_COLOR_RED, true, false);
				drawShadedOval(g2d, srect.x - 1, srect.y + srect.height - circleHeight - 1, srect.width + 2, circleHeight + 2, RING_COLOR_ORANGE, false, false);
			}
			
			// Get the direction of his opponents and draw an arrow to those
			EnumSet<Direction> directions = EnumSet.noneOf(Direction.class);
			for (Entity attacker : entity.getAttackers()) {
				directions.add(Direction.getAreaDirectionTowardsArea(entity.getArea(), attacker.getArea()));	
			}
			drawArrows(g2d, srect.x - 1, srect.y + srect.height - circleHeight - 1, srect.width + 2, circleHeight + 2, directions, lineColor);
		
		// When the entity is attacked by the user, but still is attacking the user, give him a dark orange outline
		} else if (entity.isAttacking(User.get())) {
			drawShadedOval(g2d, srect.x - 1, srect.y + srect.height - circleHeight - 1, srect.width + 2, circleHeight + 2, RING_COLOR_ORANGE, false, false);
		}
		
		drawAttack(g2d, x, y, width, height);
		
		if (entity.isDefending()) {
			// Draw bottom right combat icon
			final int sx = srect.x + srect.width - ICON_OFFSET;
			final int sy = y + height - 2 * ICON_OFFSET;

			switch (entity.getResolution()) {
			case BLOCKED:
				blockedSprite.draw(g2d, sx, sy);
				break;

			case MISSED:
				missedSprite.draw(g2d, sx, sy);
				break;

			case HIT:
				hitSprite.draw(g2d, sx, sy);
				break;
			default:
				// cannot happen we are switching on enum
			}
		}
	}
	
	/**
	 * Function to draw the arrows on the attack/being attacked ring.
	 * 
	 * @param g2d The graphic context
	 * @param x The x-center of the arrows
	 * @param y The y-center of the arrows
	 * @param width ring width
	 * @param height ring height
	 * @param directions The directions an arrow should be drawn
	 * @param lineColor The color of the outline of the arrow
	 */
	private void drawArrows(final Graphics2D g2d, final int x, final int y, final int width, final int height, final EnumSet<Direction> directions, final Color lineColor) {
		int arrowHeight = 6 + 2 * (height / 23 - 1);
		int arrowWidth = 3 + (width / 34 - 1);
		if (directions.contains(Direction.LEFT)) {
			g2d.setColor(Color.RED);
			g2d.fillPolygon(
					new int[] {x+1, x-arrowWidth, x+1},
					new int[]{y+(height/2)-(arrowHeight/2), y+(height/2),y+(height/2)+(arrowHeight/2)},
					3);
			g2d.setColor(lineColor);
			g2d.drawPolyline(
					new int[]{x, x-arrowWidth, x},
					new int[]{y+(height/2)-(arrowHeight/2), y+(height/2), y+(height/2)+(arrowHeight/2)},
					3);
		}
		if (directions.contains(Direction.RIGHT)) {
			g2d.setColor(Color.RED);
			g2d.fillPolygon(
					new int[]{x+width, x+width+arrowWidth, x+width},
					new int[]{y+(height/2)-(arrowHeight/2), y+(height/2), y+(height/2)+(arrowHeight/2)},
					3);
			g2d.setColor(lineColor);
			g2d.drawPolyline(
					new int[]{x+width, x+width+arrowWidth, x+width},
					new int[]{y+(height/2)-(arrowHeight/2), y+(height/2), y+(height/2)+(arrowHeight/2)},
					3);
		}
		if (directions.contains(Direction.UP)) {
			g2d.setColor(Color.RED);
			g2d.fillPolygon(
					new int[]{x+(width/2)-(arrowHeight/2), x+(width/2), x+(width/2)+(arrowHeight/2)},
					new int[]{y+1, y-arrowWidth, y+1},
					3);
			g2d.setColor(lineColor);
			g2d.drawPolyline(
					new int[]{x+(width/2)-(arrowHeight/2), x+(width/2), x+(width/2)+(arrowHeight/2)},
					new int[]{y, y-arrowWidth, y},
					3);
		}
		if (directions.contains(Direction.DOWN)) {
			g2d.setColor(Color.RED);
			g2d.fillPolygon(
					new int[]{x+(width/2)-(arrowHeight/2), x+(width/2), x+(width/2)+(arrowHeight/2)},
					new int[]{y+height, y+height+arrowWidth, y+height},
					3);
			g2d.setColor(lineColor);
			g2d.drawPolyline(
					new int[]{x+(width/2)-(arrowHeight/2), x+(width/2), x+(width/2)+(arrowHeight/2)},
					new int[]{y+height, y+height+arrowWidth, y+height},
					3);
		}
	}
	
	/**
	 * @param g2d The graphic context
	 * @param x The x-position of the upperleft of the oval
	 * @param y The y-position of the upperleft of the oval
	 * @param width The widht of the oval
	 * @param height The height of the oval
	 * @param color The base color of the oval, shadow still needs to be applied
	 * @param reversed Whether the bottom part, or the upper part should be dark (true is upper part)
	 * @param light
	 */
	private void drawShadedOval(final Graphics2D g2d, final int x, final int y, final int width, final int height, final Color color, final boolean reversed, final boolean light) {
		
		// Calculate how much darker the ring must be made (depends on the boolean 'light')
		float multi1;
		float multi2;
		if (light) {
			multi1 = reversed ? 1f : 0.8f;
			multi2 = reversed ? 0.8f : 1f;
		} else {
			multi1 = reversed ? 0.24f : 0.39f;
			multi2 = reversed ? 0.39f : 0.24f;
		}
		
		// Darken the colors by the given multiplier
		Color color1 = new Color((int) (color.getRed() * multi1), (int) (color.getGreen() * multi1), (int) (color.getBlue() * multi1));
		Color color2 = new Color((int) (color.getRed() * multi2), (int) (color.getGreen() * multi2), (int) (color.getBlue() * multi2));
		
		// Draw with two arcs a oval
		g2d.setColor(color1);
		g2d.drawArc(x, y, width, height, 0, 180);
		g2d.setColor(color2);
		g2d.drawArc(x, y, width, height, 180, 180);
	}
	
	/**
	 * Draw the attacking effect.
	 * 
	 * @param g2d The graphics context 
	 * @param x x coordinate of the attacker
	 * @param y y coordinate of the attacker
	 * @param width width of the attacker
	 * @param height height of the attacker
	 */
	private void drawAttack(final Graphics2D g2d, final int x, final int y, final int width, final int height) {
		if (isAttacking) {
			if (!attackPainter.isDoneAttacking()) {
				RPEntity target = entity.getAttackTarget();
				
				if (target != null) {
					if (rangedAttack) {
						attackPainter.drawDistanceAttack(g2d, entity, target, x, y, width, height);
					} else {
						attackPainter.draw(g2d, entity.getDirection(), x, y, width, height);
					}
				}
			} else {
				isAttacking = false;
			}
		}
	}	

	/**
	 * Get the full directional animation tile set for this entity.
	 * 
	 * @return A tile sprite containing all animation images.
	 */
	protected abstract Sprite getAnimationSprite();

	/**
	 * Get the number of tiles in the X axis of the base sprite.
	 * 
	 * @return The number of tiles.
	 */
	protected int getTilesX() {
		return 3;
	}

	/**
	 * Get the number of tiles in the Y axis of the base sprite.
	 * 
	 * @return The number of tiles.
	 */
	protected int getTilesY() {
		return 4;
	}

	/**
	 * Determine is the user can see this entity while in ghostmode.
	 * 
	 * @return <code>true</code> if the client user can see this entity while in
	 *         ghostmode.
	 */
	protected boolean isVisibleGhost() {
		return false;
	}

	//
	// StateEntity2DView
	//

	/**
	 * Populate keyed state sprites.
	 * 
	 * @param entity the entity to build sprites for
	 * @param map
	 *            The map to populate.
	 */
	@Override
	protected void buildSprites(T entity, final Map<Object, Sprite> map) {
		final Sprite tiles = getAnimationSprite();

		width = tiles.getWidth() / getTilesX();
		height = tiles.getHeight() / getTilesY();

		buildSprites(map, tiles, width, height);
		calculateOffset(entity, width, height);
		
		/*
		 * Set icons for a newly created entity.
		 * These need to be after the main sprite is ready, so that the
		 * dimensions are correct for the sprite placement.
		 */
		for (StatusIconManager handler : iconManagers) {
			handler.check(entity);
		}
		
		// Prepare the health bar
		int barWidth = Math.max(width * 2 / 3, IGameScreen.SIZE_UNIT_PIXELS);
		healthBar = new HealthBar(barWidth, HEALTH_BAR_HEIGHT);
		healthBar.setHPRatio(entity.getHpRatio());
	}

	//
	// Entity2DView
	//

	/**
	 * Build a list of entity specific actions. <strong>NOTE: The first entry
	 * should be the default.</strong>
	 * 
	 * @param list
	 *            The list to populate.
	 */
	@Override
	protected void buildActions(final List<String> list) {
		super.buildActions(list);

		/*
		 * Menu is used to provide an alternate action for some entities (like
		 * puppies - and they should not be attackable).
		 * 
		 * For now normally attackable entities get a menu only in Capture The
		 * Flag, and then they don't need attack. If that changes, this code
		 * will need to be adjusted.
		 */
		if (!entity.getRPObject().has("menu")) {
			if (entity.isAttackedBy(User.get())) {
				list.add(ActionType.STOP_ATTACK.getRepresentation());
			} else {
				list.add(ActionType.ATTACK.getRepresentation());
			}
		}

		list.add(ActionType.PUSH.getRepresentation());
	}

	/**
	 * Draw the entity.
	 * 
	 * @param g2d
	 *            The graphics context.
	 * @param x
	 *            The drawn X coordinate.
	 * @param y
	 *            The drawn Y coordinate.
	 * @param width
	 *            The drawn entity width.
	 * @param height
	 *            The drawn entity height.
	 */
	@Override
	protected void draw(final Graphics2D g2d, final int x, final int y,
			final int width, final int height) {
		drawCombat(g2d, x, y, width, height);
		super.draw(g2d, x, y, width, height);

		if (Debug.SHOW_ENTITY_VIEW_AREA) {
			g2d.setColor(Color.cyan);
			g2d.drawRect(x, y, width, height);
		}

		drawFloaters(g2d, x, y, width);
	}

	/**
	 * Draw the top layer parts of an entity. This will be on down after all
	 * other game layers are rendered.
	 * 
	 * @param g2d
	 *            The graphics context.
	 * @param x
	 *            The drawn X coordinate.
	 * @param y
	 *            The drawn Y coordinate.
	 * @param width
	 *            The drawn entity width.
	 * @param height
	 *            The drawn entity height.
	 */
	@Override
	protected void drawTop(final Graphics2D g2d, final int x, final int y,
			final int width, final int height) {
		drawStatusBar(g2d, x, y, width);
	}

	/**
	 * Get the height.
	 * 
	 * @return The height (in pixels).
	 */
	@Override
	public int getHeight() {
		return height;
	}

	/**
	 * Get the entity's visibility.
	 * 
	 * @return The visibility value (0-100).
	 */
	@Override
	protected int getVisibility() {
		/*
		 * Hide while in ghostmode.
		 */
		if (entity.isGhostMode()) {
			if (isVisibleGhost()) {
				return super.getVisibility() / 2;
			} else {
				return 0;
			}
		} else {
			return super.getVisibility();
		}
	}

	/**
	 * Get the width.
	 * 
	 * @return The width (in pixels).
	 */
	@Override
	public int getWidth() {
		return width;
	}

	/**
	 * Determines on top of which other entities this entity should be drawn.
	 * Entities with a high Z index will be drawn on top of ones with a lower Z
	 * index.
	 * 
	 * Also, players can only interact with the topmost entity.
	 * 
	 * @return The drawing index.
	 */
	@Override
	public int getZIndex() {
		return 8000;
	}

	@Override
	protected void update() {
		super.update();

		if (titleChanged) {
			showTitle = entity.showTitle();
			if (showTitle) {
				titleSprite = createTitleSprite();
			} else {
				titleSprite = null;
			}
			titleChanged = false;
		}
	}

	@Override
	void entityChanged(final Object property) {
		super.entityChanged(property);

		if (property == RPEntity.PROP_ADMIN_LEVEL) {
			titleChanged = true;
			visibilityChanged = true;
		} else if (property == RPEntity.PROP_GHOSTMODE) {
			visibilityChanged = true;
		} else if (property == RPEntity.PROP_OUTFIT) {
			representationChanged = true;
		} else if (property == IEntity.PROP_TITLE) {
			titleChanged = true;
		} else if (property == RPEntity.PROP_TITLE_TYPE) {
			titleChanged = true;
		} else if (property == RPEntity.PROP_TEXT_INDICATORS) {
			onFloatersChanged();
		} else if (property == RPEntity.PROP_HP_RATIO) {
			if (healthBar != null) {
				healthBar.setHPRatio(entity.getHpRatio());
			}
		} else if (property == RPEntity.PROP_HP_DISPLAY) {
			showHP = entity.showHPBar();
		} else if (property == RPEntity.PROP_ATTACK) {
			Nature nature = entity.getShownDamageType();
			if (nature == null) {
				isAttacking = false;
			} else {
				rangedAttack = entity.isDoingRangedAttack();
				if (attackPainter == null || !attackPainter.hasNature(nature)) {
					attackPainter = AttackPainter.get(nature, (int) Math.min(entity.getWidth(), entity.getHeight()));	
				}
				attackPainter.prepare(getState(entity));
				isAttacking = true;
			}
		}
		
		for (StatusIconManager handler : iconManagers) {
			handler.check(property, entity);
		}
	}

	/**
	 * Called when the floating text indicators change.
	 */
	private void onFloatersChanged() {
		Iterator<TextIndicator> it = entity.getTextIndicators();
		Map<TextIndicator, Sprite> newFloaters = new HashMap<TextIndicator, Sprite>();
		
		while (it.hasNext()) {
			TextIndicator floater = it.next();
			Sprite sprite = floaters.get(floater);
			if (sprite == null) {
				sprite = TextSprite.createTextSprite(floater.getText(), floater.getType().getColor());
			}
			
			newFloaters.put(floater, sprite);
		}
		
		floaters = newFloaters;
	}

	/**
	 * Perform the default action.
	 */
	@Override
	public void onAction() {
		if (entity.getRPObject().has("menu")) {
			onAction(ActionType.USE);
		} else {
			super.onAction();
		}
	}

	/**
	 * Perform an action.
	 * 
	 * @param action
	 *            The action.
	 */
	@Override
	public void onAction(ActionType action) {
		ActionType at = action;
		if (at == null) {
			at = ActionType.USE;
		}
		if (isReleased()) {
			return;
		}
		RPAction rpaction;

		switch (at) {
		case ATTACK:
		case PUSH:
		case USE:
			at.send(at.fillTargetInfo(entity));
			break;

		case STOP_ATTACK:
			rpaction = new RPAction();

			rpaction.put("type", at.toString());
			rpaction.put("attack", "");

			at.send(rpaction);
			break;

		default:
			super.onAction(at);
			break;
		}
	}
	
	/**
	 * A manager for a status icon. Observes property changes and shows and
	 * hides the icon as needed.
	 */
	abstract class StatusIconManager {
		/** Observed property */
		private final Object property;
		/** Icon sprite */
		private final Sprite sprite;
		private final HorizontalAlignment xAlign;
		private final VerticalAlignment yAlign;
		private final int xOffset, yOffset;
		/**
		 * For tracking changes that can have multiple "true" values (such
		 * as away messages).
		 */
		private boolean wasVisible;
		
		/**
		 * Create a new StatusIconManager.
		 * 
		 * @param property observed property
		 * @param sprite icon sprite
		 * @param xAlign
		 * @param yAlign
		 * @param xOffset
		 * @param yOffset
		 */
		StatusIconManager(Object property, Sprite sprite,
				HorizontalAlignment xAlign, VerticalAlignment yAlign,
				int xOffset, int yOffset) {
			this.property = property;
			this.sprite = sprite;
			this.xAlign = xAlign;
			this.yAlign = yAlign;
			this.xOffset = xOffset;
			this.yOffset = yOffset;
		}
		
		/**
		 * Check if the icon should be shown.
		 * 
		 * @param entity
		 * @return <code>true</code> if the icon should be shown,
		 * 	<code>false</code> otherwise
		 */
		abstract boolean show(T entity);
		
		/**
		 * Check the entity at a property change. Show or hide the icon if
		 * needed.
		 * 
		 * @param changedProperty
		 * @param entity
		 */
		private void check(Object changedProperty, T entity) {
			if (property == changedProperty) {
				check(entity);
			}
		}
		
		/**
		 * Check the status of an entity, and show or hide the icon if
		 * needed.
		 * 
		 * @param entity
		 */
		private void check(T entity) {
			setVisible(show(entity));
		}
		
		/**
		 * Attach or detach the icon sprite, depending on visibility.
		 * 
		 * @param visible
		 */
		private void setVisible(boolean visible) {
			if (visible) {
				// Avoid attaching the sprite more than once
				if (!wasVisible) {
					attachSprite(sprite, xAlign, yAlign, xOffset, yOffset);
					wasVisible = true;
				}
			} else {
				wasVisible = false;
				detachSprite(sprite);
			}
		}
	}
}
