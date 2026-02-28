try {
    var clusterName = "dockercluster";
    var cluster;

    print(">>> [START] MySQL InnoDB Cluster Configuration");

    // 1. 클러스터 가져오기 또는 생성
    try {
        cluster = dba.getCluster(clusterName);
        print(">>> Found existing cluster: " + clusterName);
    } catch (e) {
        print(">>> Creating new cluster: " + clusterName);
        // node1에서 실행하므로 localhost 접속 상태를 가정
        cluster = dba.createCluster(clusterName);
    }

    // 2. 노드 추가 함수 (중복 체크 포함)
    function addNode(nodeUri) {
        var status = cluster.status();
        if (!status.defaultReplicaSet.topology[nodeUri.split("@")[1]]) {
            print(">>> Adding instance: " + nodeUri);
            cluster.addInstance(nodeUri, {recoveryMethod: "clone"});
            print(">>> Instance " + nodeUri + " added successfully.");
        } else {
            print(">>> Instance " + nodeUri + " is already a member.");
        }
    }

    // 3. 노드 2, 3 추가
    addNode("root:1234@fisa-mysql-node2:3306");
    addNode("root:1234@fisa-mysql-node3:3306");

    // 4. 최종 상태 출력 (작업 완료 시 결과 보여주기)
    print(">>> [COMPLETE] Final Cluster Status:");
    print(JSON.stringify(cluster.status(), null, 2));

} catch (err) {
    print(">>> [ERROR] Cluster operation failed: " + err.message);
}