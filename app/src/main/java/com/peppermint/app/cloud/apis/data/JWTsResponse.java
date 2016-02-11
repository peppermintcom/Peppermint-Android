package com.peppermint.app.cloud.apis.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nuno Luz on 28-01-2016.
 *
 * Data wrapper for HTTP /jwts endpoint responses.
 */
public class JWTsResponse extends AccessTokenResponse {

    private List<AccountsResponse> mAccountList;
    private List<RecorderResponse> mRecorderList;

    public JWTsResponse() {
        mAccountList = new ArrayList<>();
        mRecorderList = new ArrayList<>();
    }

    public void addAccount(AccountsResponse accountResponse) {
        mAccountList.add(accountResponse);
    }

    public List<AccountsResponse> getAccountList() {
        return mAccountList;
    }

    public void setAccountList(List<AccountsResponse> mAccountList) {
        this.mAccountList = mAccountList;
    }

    public AccountsResponse getAccount() {
        if(mAccountList.size() <= 0) {
            return null;
        }
        return mAccountList.get(0);
    }

    public void addRecorder(RecorderResponse recorderResponse) {
        mRecorderList.add(recorderResponse);
    }

    public List<RecorderResponse> getRecorderList() {
        return mRecorderList;
    }

    public void setRecorderList(List<RecorderResponse> mRecorderList) {
        this.mRecorderList = mRecorderList;
    }

    public RecorderResponse getRecorder() {
        if(mRecorderList.size() <= 0) {
            return null;
        }
        return mRecorderList.get(0);
    }

    @Override
    public String toString() {
        return "JWTsResponse{" +
                "mAccountList=" + mAccountList +
                ", mRecorderList=" + mRecorderList +
                '}';
    }
}
