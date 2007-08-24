package games.stendhal.server.maps.quests;

import games.stendhal.common.Grammar;
import games.stendhal.server.StendhalRPWorld;
import games.stendhal.server.entity.item.Item;
import games.stendhal.server.entity.npc.ConversationPhrases;
import games.stendhal.server.entity.npc.ConversationStates;
import games.stendhal.server.entity.npc.SpeakerNPC;
import games.stendhal.server.entity.player.Player;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * QUEST: ElvishArmor
 *
 * PARTICIPANTS: - Lupos, an albino elf who live in Fado Forest
 *
 * STEPS: - Lupos wants to see every piece of elvish equipment you can bring him
 *
 * REWARD: - 10000 XP - 15 Karma - ability to sell elvish stuff and also drow sword
 *
 * REPETITIONS: - None.
 */
public class ElvishArmor extends AbstractQuest {

	private static final List<String> NEEDEDITEMS = Arrays.asList("elvish_armor", "elvish_legs", "elvish_boots", "elvish_sword", "elvish_cloak");

	/**
	 * Returns a list of the names of all items that the given player still has
	 * to bring to fulfil the quest.
	 *
	 * @param player
	 *            The player doing the quest
	 * @param hash
	 *            If true, sets a # character in front of every name
	 * @return A list of item names
	 */
	private List<String> missingitems(Player player, boolean hash) {
		List<String> result = new LinkedList<String>();

		String doneText = player.getQuest("elvish_armor");
		if (doneText == null) {
			doneText = "";
		}
		List<String> done = Arrays.asList(doneText.split(";"));
		for (String item : NEEDEDITEMS) {
			if (!done.contains(item)) {
				if (hash) {
					item = "#" + item;
				}
				result.add(item);
			}
		}
		return result;
	}

	private void step_1() {
		SpeakerNPC npc = npcs.get("Lupos");

		// player says hi before starting the quest
		npc
				.add(
						ConversationStates.IDLE,
						ConversationPhrases.GREETING_MESSAGES,
						new SpeakerNPC.ChatCondition() {
							@Override
							public boolean fire(Player player, String text,
									SpeakerNPC engine) {
								return !player.hasQuest("elvish_armor");
							}
						},
						ConversationStates.ATTENDING,
						"Greetings, traveller. I see that you have come far to be here. I am interested in anyone who has encountered our kin, the green #elves",
						null);

		npc.add(ConversationStates.ATTENDING, "elves",
				new SpeakerNPC.ChatCondition() {
					@Override
					public boolean fire(Player player, String text,
							SpeakerNPC engine) {
						return !player.hasQuest("elvish_armor");
					}
				}, ConversationStates.QUEST_OFFERED, "Yes, those that dwell in the forest of Nalwor. They guard their #secrets closely, you know.", null);
		// player asks what items are needed 
		npc.add(ConversationStates.QUEST_OFFERED, "secrets", null,
				ConversationStates.QUEST_OFFERED,"They won't share knowledge of how to create the green armor. You would call it elvish armor. I wonder if a traveller like you could bring me any?",null);
		// player says yes 
		npc.add(ConversationStates.QUEST_OFFERED, 
				ConversationPhrases.YES_MESSAGES, null, 
				ConversationStates.IDLE, null, new SpeakerNPC.ChatAction() {
					@Override
					public void fire(Player player, String text, 
							SpeakerNPC engine) { 
						engine.say("The secrets of the green elves shall be ours at last! Bring me all elvish equipment you can find, I'll reward you well!"); 
						player.setQuest("elvish_armor", "");
						player.addKarma(5.0);
					}
				});

		// player is not willing to help
		npc.add(ConversationStates.QUEST_OFFERED, "no", null,
				ConversationStates.QUEST_OFFERED, null,
				new SpeakerNPC.ChatAction() {
					@Override
					public void fire(Player player, String text,
							SpeakerNPC engine) {
						engine.say("Another unhelpful soul, I see.");
						player.addKarma(-5.0);
					}
				});
	     
	}

	private void step_2() {
		// Just find the items by killing elves and/or dark elves and bring them to Lupos.
	}

	private void step_3() {
		SpeakerNPC npc = npcs.get("Lupos");

		// player returns while quest is still active
		npc.add(
				ConversationStates.IDLE,
				ConversationPhrases.GREETING_MESSAGES,
				new SpeakerNPC.ChatCondition() {
					@Override
					public boolean fire(Player player, String text,
							SpeakerNPC engine) {
						return player.hasQuest("elvish_armor")
								&& !player.isQuestCompleted("elvish_armor");
					}
				}, ConversationStates.QUESTION_1,
				"Hello! I hope your search for elvish #equipment is going well?", null);
		// player asks what exactly is missing
		npc.add(ConversationStates.QUESTION_1, "equipment", null,
				ConversationStates.QUESTION_1, null,
				new SpeakerNPC.ChatAction() {
					@Override
					public void fire(Player player, String text,
							SpeakerNPC engine) {
						List<String> needed = missingitems(player, true);
						engine.say("I have heard descriptions of "
								+ Grammar
										.quantityplnoun(needed.size(), "item")
								+ " in all. They are: "
								+ Grammar.enumerateCollection(needed)
								+ ". Have you looted any?");
					}
				});
		// player says he has a required item with him
		npc.add(ConversationStates.QUESTION_1,
				ConversationPhrases.YES_MESSAGES, null,
				ConversationStates.QUESTION_1,
				"Felicitations! What #equipment did you pillage?", null);

		for (String item : NEEDEDITEMS) {
			npc.add(ConversationStates.QUESTION_1, item, null,
					ConversationStates.QUESTION_1, null,
					new SpeakerNPC.ChatAction() {
						@Override
						public void fire(Player player, String text,
								SpeakerNPC engine) {
							List<String> missing = missingitems(player, false);
							if (missing.contains(text)) {
								if (player.drop(text)) {
									// register item as done
									String doneText = player
											.getQuest("elvish_armor");
									player.setQuest("elvish_armor",
											doneText + ";" + text);
									// check if the player has brought all
									// items
									missing = missingitems(player, true);
									if (missing.size() > 0) {
										engine
												.say("Excellent work. Is there more that you plundered?");
									} else {
									    //	Item blackitem = StendhalRPWorld.get()
									    //			.getRuleManager()
									    //			.getEntityManager().getItem(
									    //					"black_item");
									    //	blackitem.put("bound", player
									    //			.getName());
									//	player.equip(blackitem, true);
										player.addKarma(10.0);
										player.addXP(10000);
										engine
												.say("I will study these! The albino elves owe you a debt of thanks.");
										player.setQuest("elvish_armor",
												"done");
										player.notifyWorldAboutChanges();
										engine.setCurrentState(ConversationStates.ATTENDING);
									}
								} else {
									engine
											.say("Liar! You don't really have "
													+ Grammar.a_noun(text)
													+ " with you.");
								}
							} else {
								engine
										.say("You've already brought that elvish item to me.");
							}
						}
					});
		}

		npc.add(ConversationStates.QUESTION_1, "",
				new SpeakerNPC.ChatCondition() {
					@Override
					public boolean fire(Player player, String text,
							SpeakerNPC engine) {
						return !NEEDEDITEMS.contains(text);
					}
				}, ConversationStates.QUESTION_1, "I don't think that's a piece of elvish armor...",
				null);

		npc.add(ConversationStates.ATTENDING, "no",
				new SpeakerNPC.ChatCondition() {
					@Override
					public boolean fire(Player player, String text,
							SpeakerNPC engine) {
						return !player.isQuestCompleted("elvish_armor");
					}
				}, ConversationStates.ATTENDING,
				"I understand, the green elves protect themselves well. If there's anything else I can do for you, just say.", null);

		// player says he didn't bring any items to different question
		npc.add(ConversationStates.QUESTION_1, Arrays.asList("no", "nothing"),
				new SpeakerNPC.ChatCondition() {
					@Override
					public boolean fire(Player player, String text,
							SpeakerNPC engine) {
						return !player.isQuestCompleted("elvish_armor");
					}
				}, ConversationStates.ATTENDING, "I see. If there's anything else I can do for you, just say.",
				null);

		// player returns after finishing the quest
		npc.add(ConversationStates.IDLE, ConversationPhrases.GREETING_MESSAGES,
				new SpeakerNPC.ChatCondition() {
					@Override
					public boolean fire(Player player, String text,
							SpeakerNPC engine) {
						return player.isQuestCompleted("elvish_armor");
					}
				}, ConversationStates.ATTENDING,
				"Greetings again, old friend.", null);

		// player returns after finishing the quest and says offer 
		npc.add(ConversationStates.ATTENDING, ConversationPhrases.OFFER_MESSAGES,
				new SpeakerNPC.ChatCondition() {
					@Override
					public boolean fire(Player player, String text,
							SpeakerNPC engine) {
						return player.isQuestCompleted("elvish_armor");
					}
				}, ConversationStates.ATTENDING,
				"If you have found any more elvish armor, I'd be glad if you would #sell it to me.", null);	
	// player returns after finishing the quest and says quest
		npc.add(ConversationStates.ATTENDING, ConversationPhrases.QUEST_MESSAGES,
				new SpeakerNPC.ChatCondition() {
					@Override
					public boolean fire(Player player, String text,
							SpeakerNPC engine) {
						return player.isQuestCompleted("elvish_armor");
					}
				}, ConversationStates.ATTENDING,
				"I'm now busy studying the properties of the elvish armor you brought me. It really is intriguing. Until I can reproduce it, I would buy similar items from you.", null);

	// player returns when the quest is in progress and says quest
		npc.add(ConversationStates.ATTENDING, ConversationPhrases.QUEST_MESSAGES,
				new SpeakerNPC.ChatCondition() {
					@Override
					public boolean fire(Player player, String text,
							SpeakerNPC engine) {
						return player.hasQuest("elvish_armor")
								&& !player.isQuestCompleted("elvish_armor");
					}
				}, ConversationStates.QUESTION_1,
				"As you already know, I seek elvish #equipment.", null);
	

	// player returns when the quest is in progress and says offer
		npc.add(ConversationStates.ATTENDING, ConversationPhrases.OFFER_MESSAGES,
				new SpeakerNPC.ChatCondition() {
					@Override
					public boolean fire(Player player, String text,
							SpeakerNPC engine) {
						return player.hasQuest("elvish_armor")
								&& !player.isQuestCompleted("elvish_armor");
					}
				}, ConversationStates.ATTENDING,
				"I don't think I trust you well enough yet ... ", null);
	}
	@Override
	public void addToWorld() {
		super.addToWorld();
		step_1();
		step_2();
		step_3();
	}
}
