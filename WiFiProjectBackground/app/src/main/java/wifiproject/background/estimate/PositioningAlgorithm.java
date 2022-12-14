package wifilocation.background.estimate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import wifilocation.background.database.EstimatedResult;
import wifilocation.background.database.ItemInfo;

public class PositioningAlgorithm {
    static List<RecordPoint> tp;
    static List<RecordPoint> rp;
    static List<ItemInfo> previousDatabase = null;

    static String lastMethod = "";
    static int lastGHZ = 0;
    static int lastK = 0;
    static int lastMinValidAPNum = 0;
    static int lastMinDbm = 0;

    public static EstimatedResult run(List<ItemInfo> userData, List<ItemInfo> databaseData, String targetBuilding, String targetSSID, String targetUUID, String method, int targetGHZ, double standardRecordDistance) {
        int K = 0;
        int minValidAPNum = 0;
        int minDbm = 0;

        if (method.equals("WiFi") && targetGHZ == 2) {
            K = 7;
            minValidAPNum = 1;
            minDbm = -50;
        } else if (method.equals("WiFi") && targetGHZ == 5) {
            K = 7;
            minValidAPNum = 1;
            minDbm = -55;
        } else if (method.equals("BLE")) {
            K = 3;
            minValidAPNum = 1;
            minDbm = -70;
        } else if (method.equals("iBeacon")) {
            K = 3;
            minValidAPNum = 1;
            minDbm = -70;
        } else {
            return null;
        }

        return runKNN(userData, databaseData, targetBuilding, targetSSID, targetUUID, method, targetGHZ, standardRecordDistance, K, minValidAPNum, minDbm);
    }

    public static EstimatedResult runKNN(List<ItemInfo> userData, List<ItemInfo> databaseData, String targetBuilding, String targetSSID, String targetUUID, String method, int targetGHZ, double standardRecordDistance, int K, int minValidAPNum, int minDbm) {
        // 데이터베이스는 한 줄에 하나의 AP 정보가 담겨있기 때문에
        // 이것을 다루기 쉽게 한 측정 지점에서 측정한 RSSI 값들을 모두 하나의 RecordPoint 객체에 담아주는 과정입니다.
        // 데이터베이스에 대한 작업은 기존에 변환한 정보가 없거나 받은 데이터베이스 정보가 변경되었을 때만 시행합니다.
        tp = getRecordPointList(userData, targetBuilding, method, targetSSID, targetGHZ, minDbm);
        if (tp.size() == 0) {
            return null;
        }

        if (databaseData != previousDatabase || !method.equals(lastMethod) || lastGHZ != targetGHZ || lastK != K || lastMinValidAPNum != minValidAPNum || lastMinDbm != minDbm) {
            rp = getRecordPointList(databaseData, targetBuilding, method, targetSSID, targetGHZ, minDbm);

            previousDatabase = databaseData;
            lastMethod = method;
            lastGHZ = targetGHZ;
            lastK = K;
            lastMinValidAPNum = minValidAPNum;
            lastMinDbm = minDbm;
        }

        // 변환된 정보를 함수에 넣어서 추정값을 반환받습니다.
        EstimatedResult estimatedResult = new EstimatedResult(targetBuilding, targetSSID, targetUUID, method + "-" + targetGHZ + "Ghz", K, minDbm, 1);
        double[] positionResult = estimate(tp.get(0), rp, K, minValidAPNum, minDbm, standardRecordDistance);
        if (positionResult == null) {
            return null;
        }

        estimatedResult.setEst_x(positionResult[0]);
        estimatedResult.setEst_y(positionResult[1]);

        return estimatedResult;
    }

    static List<RecordPoint> getRecordPointList(List<ItemInfo> databaseData, String targetBuilding, String method, String targetSSID, int targetGHZ, int minDbm) {
        List<RecordPoint> rp = new ArrayList<>();

        for (ItemInfo databaseRow : databaseData) {
            RecordPoint workingRP = null;

            // 유사 위치를 동일 좌표로 간주하기 위해서 스캔 좌표 소수점을 반올림 처리 (현실적 판단)
            databaseRow.setPos_x((float) Math.round(databaseRow.getPos_x()));
            databaseRow.setPos_y((float) Math.round(databaseRow.getPos_y()));

            if (!targetBuilding.equals(databaseRow.getBuilding())
                    || !method.equals(databaseRow.getMethod())
                    || !targetSSID.equals(databaseRow.getSSID())
                    || method.equals("WiFi") && databaseRow.getFrequency() / 1000 != targetGHZ
                    || databaseRow.getLevel() < minDbm) {
                continue;
            }

            for (RecordPoint recordPoint : rp) {
                if (databaseRow.getPos_x() == recordPoint.getLocation()[0] && databaseRow.getPos_y() == recordPoint.getLocation()[1]) {
                    workingRP = recordPoint;

                    break;
                }
            }

            if (workingRP == null) {
                workingRP = new RecordPoint(new double[]{databaseRow.getPos_x(), databaseRow.getPos_y()});
                rp.add(workingRP);
            }
            workingRP.getRSSI().put(databaseRow.getBSSID(), databaseRow.getLevel());
        }

        return rp;
    }

    static double[] estimate(RecordPoint tp, List<RecordPoint> rp, int K, int minValidAPNum, int minDbm, double standardRecordDistance) {
        List<RecordPoint> vrp = interpolation(rp, standardRecordDistance);
        return weightedKNN(tp, vrp, K, minValidAPNum, minDbm);
    }

    static List<RecordPoint> interpolation(List<RecordPoint> rp, double standardRecordDistance) {
        List<RecordPoint> vrp = new ArrayList<>(rp);

        for (int i = 0; i < rp.size(); i++) {
            for (int j = i + 1; j < rp.size(); j++) {
                double distanceSquare = 0;
                for (int k = 0; k < 2; k++) {
                    distanceSquare += Math.pow(rp.get(i).getLocation()[k] - rp.get(j).getLocation()[k], 2);
                }

                if (distanceSquare < Math.pow(standardRecordDistance, 2) * 0.7 || distanceSquare > Math.pow(standardRecordDistance, 2) * 2) {
                    continue;
                }

                Set<String> intersectBSSID = new HashSet<>(rp.get(i).getRSSI().keySet());
                intersectBSSID.retainAll(rp.get(j).getRSSI().keySet());

                RecordPoint newRP = new RecordPoint();
                for (String BSSID : intersectBSSID) {
                    newRP.getRSSI().put(BSSID, (rp.get(i).getRSSI().get(BSSID) + rp.get(j).getRSSI().get(BSSID)) / 2);
                }
                for (int k = 0; k < 2; k++) {
                    newRP.getLocation()[k] = (rp.get(i).getLocation()[k] + rp.get(j).getLocation()[k]) / 2;
                }

                vrp.add(newRP);
            }
        }

        return vrp;
    }

    static double[] weightedKNN(RecordPoint tp, List<RecordPoint> rp, int K, int minValidAPNum, int minDbm) {
        // K개의 최근접 RP를 선별하는 과정
        List<RecordPoint> candidateRP = new ArrayList<>();
        for (int i = 0; i < rp.size(); i++) {
            Set<String> intersectBSSID = new HashSet<>(rp.get(i).getRSSI().keySet());
            intersectBSSID.retainAll(tp.getRSSI().keySet());

            if (intersectBSSID.size() < minValidAPNum) {
                continue;
            }

            candidateRP.add(rp.get(i));
        }

        int maxDbm = Integer.MIN_VALUE;
        for (RecordPoint recordPoint : candidateRP) {
            int maxDbmInRecordPoint = Collections.max(recordPoint.getRSSI().values());
            if (maxDbmInRecordPoint > maxDbm) {
                maxDbm = maxDbmInRecordPoint;
            }
        }

        Map<RecordPoint, Double> nearDistance = getKNearDistance(tp, candidateRP, K, maxDbm, minDbm);
        // 아무것도 추정되지 않은 경우, 서비스 지역이 있지 않은 경우임
        if (nearDistance.size() == 0) {
            return null;
        }

        // K개의 최근접 AP를 토대로 평가용 가중치를 산정하는 과정
        Map<RecordPoint, Double> evaluateDistance = getKNearDistance(tp, new ArrayList<RecordPoint>(nearDistance.keySet()), K, maxDbm, minDbm);

        Map<RecordPoint, Double> weight = new HashMap<>();
        for (Entry<RecordPoint, Double> entry : evaluateDistance.entrySet()) {
            weight.put(entry.getKey(), 1 / (entry.getValue() + 0.01));
        }

        double totalWeight = 0;
        for (double val : weight.values()) {
            totalWeight += val;
        }

        // 최종 위치를 추정하는 과정
        double[] estimatedPosition = {0, 0};
        int nth = 0;
        for (Entry<RecordPoint, Double> entry : evaluateDistance.entrySet()) {
            double fraction = (weight.get(entry.getKey()) / totalWeight);
            for (int i = 0; i < 2; i++) {
                estimatedPosition[i] += fraction * entry.getKey().getLocation()[i];
            }
        }

        return estimatedPosition;
    }

    static Map<RecordPoint, Double> getKNearDistance(RecordPoint tp, List<RecordPoint> rp, int K, int maxDbm, int minDbm) {
        Set<String> allBSSID = new HashSet<>(tp.getRSSI().keySet());
        for (int i = 0; i < rp.size(); i++) {
            allBSSID.addAll(rp.get(i).getRSSI().keySet());
        }

        Map<RecordPoint, Double> nearDistanceSquareSum = new HashMap<>();
        double maxDistanceSquareSum = Double.MAX_VALUE;
        for (int i = 0; i < rp.size(); i++) {
            double currentDistanceSquareSum = 0;

            for (String BSSID : allBSSID) {
                if (tp.getRSSI().containsKey(BSSID) && rp.get(i).getRSSI().containsKey(BSSID) && rp.get(i).getRSSI().get(BSSID) >= minDbm) {
                    currentDistanceSquareSum += Math.pow(tp.getRSSI().get(BSSID) - rp.get(i).getRSSI().get(BSSID), 2);
                } else {
                    currentDistanceSquareSum += Math.pow(Math.abs(maxDbm - minDbm) + 10, 2);
                }

                if (nearDistanceSquareSum.size() >= K && currentDistanceSquareSum >= maxDistanceSquareSum) {
                    break;
                }
            }

            if (nearDistanceSquareSum.size() >= K && currentDistanceSquareSum >= maxDistanceSquareSum) {
                continue;
            }

            nearDistanceSquareSum.put(rp.get(i), currentDistanceSquareSum);
            if (nearDistanceSquareSum.size() > K) {
                nearDistanceSquareSum.values().remove(maxDistanceSquareSum);
            }

            maxDistanceSquareSum = Collections.max(nearDistanceSquareSum.values());
        }

        Map<RecordPoint, Double> nearDistance = new HashMap<>();
        for (Entry<RecordPoint, Double> entry : nearDistanceSquareSum.entrySet()) {
            nearDistance.put(entry.getKey(), Math.sqrt(entry.getValue()) / rp.size());
        }

        return nearDistance;
    }
}
