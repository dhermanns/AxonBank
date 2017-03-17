/*
 * Copyright (c) 2016. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.samples.bank.command;

public class SubBankAccount {

    private String bankAccountId;
    private String userName;
    private long balanceInCents;

    @SuppressWarnings("unused")
    private SubBankAccount() {
    }

    public SubBankAccount(String bankAccountId, String userName, long balanceInCents) {
        this.bankAccountId = bankAccountId;
        this.userName = userName;
        this.balanceInCents = balanceInCents;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SubBankAccount that = (SubBankAccount) o;

        if (balanceInCents != that.balanceInCents) return false;
        if (bankAccountId != null ? !bankAccountId.equals(that.bankAccountId) : that.bankAccountId != null)
            return false;
        return userName != null ? userName.equals(that.userName) : that.userName == null;
    }

    @Override
    public int hashCode() {
        int result = bankAccountId != null ? bankAccountId.hashCode() : 0;
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        result = 31 * result + (int) (balanceInCents ^ (balanceInCents >>> 32));
        return result;
    }
}
