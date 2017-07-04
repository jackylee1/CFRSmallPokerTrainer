package cfr.trainer.games.poker.games.lite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cfr.trainer.action.Action;
import cfr.trainer.games.Game;
import cfr.trainer.games.GameType;
import cfr.trainer.games.poker.BetRound;
import cfr.trainer.games.poker.BettingLimit;
import cfr.trainer.games.poker.Board;
import cfr.trainer.games.poker.Card;
import cfr.trainer.games.poker.Hand;
import cfr.trainer.games.poker.HandSingleCard;
import cfr.trainer.games.poker.PayOffCalculator;
import cfr.trainer.games.poker.PokerGameType;
import cfr.trainer.games.poker.PokerPlayer;
import cfr.trainer.games.poker.actions.CallAction;
import cfr.trainer.games.poker.actions.DealAction;
import cfr.trainer.games.poker.actions.FoldAction;
import cfr.trainer.games.poker.actions.PokerAction;
import cfr.trainer.games.poker.actions.PokerActionType;
import cfr.trainer.games.poker.actions.RaiseAction;
import cfr.trainer.games.poker.decks.Deck;

public class RoyalRhodeIsland10ChipNoLimitLite extends BaseTwoPlayerPokerLiteGame {


	public RoyalRhodeIsland10ChipNoLimitLite(int raisesPerRound) {
		super(BettingLimit.NO_LIMIT,raisesPerRound);
		this.board = new int[2];
		this.visibleBoardCards = new boolean[2];
		this.bettingLimit = BettingLimit.NO_LIMIT;
		this.betRound = BetRound.PRETURN;
		int[] startingStacks = {10,10};
		this.playerStack = startingStacks;
		this.pokerGameType = PokerGameType.RHODE_ISLAND;

	}


	@Override
	public int[] dealCards() {
//TODO
		return null;
	}

	@Override
	public List<List<Integer>> getListOfValidChanceCombinations() {
		List<List<Integer>> validCardCombinationLists = new ArrayList<List<Integer>>();
		List<Integer> cardOrder = new ArrayList<>();

		// add 10-A spades
		for (int card = 8; card < 13; card++) {
			cardOrder.add(card);
		}
		// add 10-A hearts
		for (int card = 21; card < 26; card++) {
			cardOrder.add(card);
		}
		// add 10-A clubs
		for (int card = 34; card < 39; card++) {
			cardOrder.add(card);
		}
		// add 10-A diamonds
		for (int card = 47; card < 52; card++) {
			cardOrder.add(card);
		}
		for (int card0: cardOrder) {
			for (int card1: cardOrder) {
				if (card0 == card1) {
					continue;
				}
				for (int boardCard1: cardOrder) {
					if (boardCard1 == card1 || boardCard1 == card0) {
						continue;
					}
					for (int boardCard2 : cardOrder) {
						if (boardCard2 == card1 || boardCard2 == card0 || boardCard2 == boardCard1) {
							continue;
						}
						List<Integer> validCardComination = new ArrayList<Integer>();
						validCardComination.add(card0);
						validCardComination.add(card1);
						validCardComination.add(boardCard1);
						validCardComination.add(boardCard2);
						validCardCombinationLists.add(validCardComination);
					}
				}
			}
		}

		return validCardCombinationLists;
	}
	
	@Override
	public RoyalRhodeIsland10ChipNoLimitLite setValidChanceCombinations(List<Integer> listOfChanceCombinations) {

		
		int boardCard1 = listOfChanceCombinations.get(0);
		int boardCard2 = listOfChanceCombinations.get(1);
		int card0 = listOfChanceCombinations.get(2);
		int card1 = listOfChanceCombinations.get(3);

		playerHands[0] = card0;
		playerHands[1] = card1;
	
		this.board[0]=boardCard1;
		this.board[1]=boardCard2;

		return this;
	}

	
}
