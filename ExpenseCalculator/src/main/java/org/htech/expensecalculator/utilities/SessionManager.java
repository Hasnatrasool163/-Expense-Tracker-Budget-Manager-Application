package org.htech.expensecalculator.utilities;

public class SessionManager {

    private static int CurrentUserId;
    private static String CurrentUserEmail;
    private static String CurrentUserName;
    private static int CurrentGroupId;
    private static String CurrentGroupName;


    public static int getCurrentUserId() {
        return CurrentUserId;
    }

    public static void setCurrentUserId(int currentUserId) {
        CurrentUserId = currentUserId;
    }

    public static String getCurrentUserEmail() {
        return CurrentUserEmail;
    }

    public static void setCurrentUserEmail(String currentUserEmail) {
        CurrentUserEmail = currentUserEmail;
    }

    public static String getCurrentUserName() {
        return CurrentUserName;
    }

    public static void setCurrentUserName(String currentUserName) {
        CurrentUserName = currentUserName;
    }

    public static int getCurrentGroupId() {
        return CurrentGroupId;
    }

    public static void setCurrentGroupId(int currentGroupId) {
        CurrentGroupId = currentGroupId;
    }

    public static String getCurrentGroupName() {
        return CurrentGroupName;
    }

    public static void setCurrentGroupName(String currentGroupName) {
        CurrentGroupName = currentGroupName;
    }
}
