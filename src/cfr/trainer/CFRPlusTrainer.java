package cfr.trainer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cfr.trainer.action.Action;
import cfr.trainer.games.Game;
import cfr.trainer.games.GameDescription;
import cfr.trainer.games.GameFactory;

public class CFRPlusTrainer {

	public Map<String, double[]> regretMap = new UnifiedMap<String, double[]>();
	double util = 0;
	int utilCount;
	double averageGameValue = 0;
	boolean invertPayoffs = true;

	public static void main(String[] args) throws Exception {
		int iterations = 2;
		new CFRPlusTrainer().train(GameDescription.ROYAL_TWO_CARD_10_CHIP_LITE, iterations);
	}

	public void train(GameDescription gameDescription, int iterations) throws Exception {

		Game gameStructure = GameFactory.setUpGame(gameDescription, 3);
		int[][] validChanceCombinations = gameStructure.getListOfValidChanceCombinations();
		long startTime = System.currentTimeMillis();
		for (int i = 1; i < iterations + 1; i++) {
			// long totalCombos = validChanceCombinations.length;

			printProgress(startTime, iterations, i);
			for (int[] validCombo : validChanceCombinations) {

				Game game = GameFactory.setUpGame(gameDescription, 3);
				game.startGame();
				game.setValidChanceCombinations(validCombo);
				// This is nonsense. need to declare new game for second
				// training.
				util += cfrPlus(game, 1, 1, 0, i);

				Game game2 = GameFactory.setUpGame(gameDescription, 3);
				game2.startGame();
				game2.setValidChanceCombinations(validCombo);
				util += cfrPlus(game2, 1, 1, 1, i);
			}
			averageGameValue = util / (i * validChanceCombinations.length*2);
			System.out.println("Average game value: " + averageGameValue + " Nodes: " + regretMap.size());
			addValueToCSV("Summary_utility", averageGameValue);
			if (i % 500 == 0) {
				writeStrategyMapToJSONFile(getStrategyMap(), i + "iterationsSummaryStrategy");
			}
		}
		averageGameValue = util / (iterations * validChanceCombinations.length * 2);
		System.out.println("Average game value: " + averageGameValue + "\n Nodes: " + regretMap.size());
		writeStrategyMapToJSONFile(getStrategyMap(), iterations + "iterationsActualSummaryStrategy");
	}

	private double cfrPlus(Game game, double p0, double p1, int playerToTrain, int iteration) throws Exception {
		// System.out.println("training player:" + playerToTrain);
		int player = game.getPlayerToAct();

		if (game.isAtTerminalNode()) {
			return game.getPayOffs()[game.getPlayerToAct()];
		}
		if (game.isAtChanceNode()) {
			int playerExpectedToAct = game.getPlayerToAct();
			game.performChanceAction();
			player = game.getPlayerToAct();
			if (playerExpectedToAct != player) {
				// player 0 will actually be the next to act
				invertPayoffs = false;
			}
		}

		// Get Node
//		String nodeId = game.getNodeIdWithActionMemory();
		String nodeId = game.getNodeIdWithSummaryState();
		// String nodeId = game.getNodeIdWithGameState();

		Action[] actionArray = game.constructActionArray();
		
		double[] regretSums = regretMap.get(nodeId);
		if (regretSums == null) {
			regretSums = new double[actionArray.length];
			regretMap.put(nodeId, regretSums);
		}
		// recursively call cfr

		double[] strategy = calculateStrategy(regretSums);
		int actionsAvailable = actionArray.length;
		double[] util = new double[actionsAvailable];
		double nodeUtil = 0;

		if (invertPayoffs) {
			for (int action = 0; action < actionsAvailable; action++) {
				Game copyOfGame = GameFactory.copyGame(game);
				copyOfGame.performAction(player, actionArray[action]);

				util[action] = player == 0 ? -cfrPlus(copyOfGame, p0 * strategy[action], p1, playerToTrain, iteration)
						: -cfrPlus(copyOfGame, p0, p1 * strategy[action], playerToTrain, iteration);

				nodeUtil += strategy[action] * util[action];
			}
		} else {
			for (int action = 0; action < actionsAvailable; action++) {
				Game copyOfGame = GameFactory.copyGame(game);
				copyOfGame.performAction(player, actionArray[action]);

				util[action] = player == 0 ? cfrPlus(copyOfGame, p0 * strategy[action], p1, playerToTrain, iteration)
						: cfrPlus(copyOfGame, p0, p1 * strategy[action], playerToTrain, iteration);

				nodeUtil += strategy[action] * util[action];
			}
			invertPayoffs = true;
		}

		// compute cfr
		if (playerToTrain == player) {
			for (int a = 0; a < actionsAvailable; a++) {
				double regret = util[a] - nodeUtil;
				// double regretSum = regretSums[a] + (player == 0 ? p1 : p0) *
				// regret;
				// node.regretSum[a] = Math.max(regretSum, 0);
				double weightedRegret = regret * iteration;
				double regretSum = (regretSums[a] + (player == 0 ? p1 : p0) * weightedRegret);
				regretSums[a] = Math.max(regretSum, 0);
			}
		}
		return nodeUtil;
	}

	public double getAverageGameValue() {
		return averageGameValue;
	}

	public Map<String, double[]> getRegretMap() {
		return regretMap;
	}

	private void writeStrategyMapToJSONFile(Map nodeMap, String fileName)
			throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.writerWithDefaultPrettyPrinter()
				.writeValue(new File("C:\\Users\\James\\Desktop\\StrategyMaps\\" + fileName + ".json"), nodeMap);
	}

	private double[] calculateStrategy(double[] regretSum) {
		int numOfActions = regretSum.length;
		double[] strategy = new double[numOfActions];
		double normalizingSum = 0;
		for (int a = 0; a < numOfActions; a++) {
			// only taking positive regrets
			strategy[a] = regretSum[a] > 0 ? regretSum[a] : 0;
			normalizingSum += strategy[a];
		}
		for (int a = 0; a < numOfActions; a++) {
			if (normalizingSum > 0)
				strategy[a] /= normalizingSum;
			else
				strategy[a] = 1.0 / numOfActions;
		}
		return strategy;
	}

	public Map<String, double[]> getStrategyMap() {
		Map<String, double[]> strategyMap = new HashMap<>();
		for (Entry<String, double[]> entry : regretMap.entrySet()) {
			strategyMap.put(entry.getKey(), calculateStrategy(entry.getValue()));
		}
		return strategyMap;
	}

	private static void printProgress(long startTime, long total, long current) {
		long eta = current == 0 ? 0 : (total - current) * (System.currentTimeMillis() - startTime) / current;

		String etaHms = current == 0 ? "N/A"
				: String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(eta),
						TimeUnit.MILLISECONDS.toMinutes(eta) % TimeUnit.HOURS.toMinutes(1),
						TimeUnit.MILLISECONDS.toSeconds(eta) % TimeUnit.MINUTES.toSeconds(1));

		StringBuilder string = new StringBuilder(140);
		int percent = (int) (current * 100 / total);
		string.append('\r')
				.append(String.join("", Collections.nCopies(percent == 0 ? 2 : 2 - (int) (Math.log10(percent)), " ")))
				.append(String.format(" %d%% [", percent)).append(String.join("", Collections.nCopies(percent, "=")))
				.append('>').append(String.join("", Collections.nCopies(100 - percent, " "))).append(']')
				.append(String.join("",
						Collections.nCopies((int) (Math.log10(total)) - (int) (Math.log10(current)), " ")))
				.append(String.format(" %d/%d, ETA: %s", current, total, etaHms));

		System.out.print(string);
	}

	public void addValueToCSV(String fileName, double utility) throws IOException {
		FileWriter pw = new FileWriter("C:\\Users\\James\\Desktop\\StrategyMaps\\" + fileName + ".csv", true);
		pw.append(Double.toString(utility));
		pw.append(",");
		pw.flush();
		pw.close();
	}

}
