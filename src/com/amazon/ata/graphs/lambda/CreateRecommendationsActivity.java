package com.amazon.ata.graphs.lambda;

import com.amazon.ata.graphs.dynamodb.FollowEdge;
import com.amazon.ata.graphs.dynamodb.FollowEdgeDao;
import com.amazon.ata.graphs.dynamodb.Recommendation;
import com.amazon.ata.graphs.dynamodb.RecommendationDao;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.lambda.runtime.Context;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class CreateRecommendationsActivity {

    private FollowEdgeDao followEdgeDao;
    private RecommendationDao recommendationDao;

    public CreateRecommendationsActivity(RecommendationDao recommendationDao, FollowEdgeDao followEdgeDao) {
        this.followEdgeDao = followEdgeDao;
        this.recommendationDao = recommendationDao;
    }

    public List<Recommendation> handleRequest(CreateRecommendationsRequest input, Context context) {
        if (input == null || input.getUsername() == null || input.getUsername().isEmpty()) {
            throw new InvalidParameterException("missing input");
        }
        List<Recommendation> recommendations = new ArrayList<>();
        PaginatedQueryList<FollowEdge> followEdges = followEdgeDao.getAllFollowers(input.getUsername());
        List<String> usernames = followEdges.stream().map(FollowEdge::getFromUsername).collect(Collectors.toList());
        for (String username : usernames) {
            PaginatedQueryList<FollowEdge> followsFollows = followEdgeDao.getAllFollowers(username);
            List<String> followsFollowsUsernames = followsFollows.stream().map(FollowEdge::getToUsername).collect(Collectors.toList());
            for (String followsfollows: followsFollowsUsernames) {
                if (followsfollows != input.getUsername() && !usernames.contains(followsfollows) && !recommendations.contains(followsfollows)) {
                    recommendations.add(new Recommendation(input.getUsername(), followsfollows, "active"));
                    if (recommendations.size() >= input.getLimit()) {
                        return recommendations;
                    }
                }
            }
        }
        return recommendations;
    }
}
