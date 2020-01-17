pragma solidity ^0.4.23;

import "./Table.sol";



contract Asset {
    
    uint nowday;
    int256 payid;
    event RegisterEvent(int256 ret, string account, uint256 asset_value);
    event TransferEvent(int256 ret, string from_account, string to_account, uint256 amount);
    
    constructor() public {
        createTable();
        createIdTable();
        assetCreateTable();
        nowday = now;
        payid = getIdAccount();
    }
    
    
    //asset
    function assetCreateTable() private {
        TableFactory tf = TableFactory(0x1001); 
        // 资产管理表, key : account, field : asset_value
        // |  资产账户(主键)      |     资产金额       |
        // |-------------------- |-------------------|
        // |        account      |    asset_value    |     
        // |---------------------|-------------------|
        //
        // 创建表
        tf.createTable("hw2_asset", "account", "asset_value");
    }
    function assetOpenTable() private returns(Table) {
        TableFactory tf = TableFactory(0x1001);
        Table table = tf.openTable("hw2_asset");
        return table;
    }
    function assetAdd(string account,uint256 addAccount) returns(int256,uint256){
        Table table = assetOpenTable();
        
        Condition condition = table.newCondition();
        condition.EQ("account",account);
        
        Entries entries = table.select(account,condition);
        //账户不存在   -1
        if(entries.size() == 0) {return (-2,1);}
        Entry entry = table.newEntry();
        entry.set("asset_value",entries.get(0).getInt("asset_value")+int256(addAccount));
        //0则更新失败   1成功
        int count = table.update(account,entry,condition);
        return (count,1);
    }
    /*function assetTake(string account,uint256 takeAccount) returns(int){
        Table table = assetOpenTable();
        
        Condition condition = table.newCondition();
        condition.EQ("account",account);
        
        Entries entries = table.select(account,condition);
        //账户不存在   -2
        if(entries.size() == 0) {return -2;}
        //账户余额不足 -1
        if(entries.get(0).getInt("asset_value") < int256(takeAccount)) {return -1;}
        Entry entry = table.newEntry();
        entry.set("asset_value",entries.get(0).getInt("asset_value")-int256(takeAccount));
        //0则更新失败   1成功
        int count = table.update(account,entry,condition);
        return count;
    }*/
    function assetRemoveAccount(string account) returns(int256,uint256) {
        Table table = assetOpenTable();
        
        Condition condition = table.newCondition();
        condition.EQ("account",account);
        
        int count = table.remove(account,condition);
        return (count,1);
    }
    function assetSelect(string account) public constant returns(int, uint256) {
        // 打开表
        Table table = assetOpenTable();
        // 查询
        Entries entries = table.select(account, table.newCondition());
        uint256 asset_value = 0;
        if (0 == uint256(entries.size())) {
            return (-1, asset_value);
        } else {
            Entry entry = entries.get(0);
            return (0, uint256(entry.getInt("asset_value")));
        }
    }
    function assetRegister(string account, uint256 asset_value) public returns(int){
        int ret_code = 0;
        int256 ret= 0;
        uint256 temp_asset_value = 0;
        // 查询账户是否存在
        (ret, temp_asset_value) = assetSelect(account);
        if(ret != 0) {
            Table table = assetOpenTable();
            
            Entry entry = table.newEntry();
            entry.set("account", account);
            entry.set("asset_value", int256(asset_value));
            // 插入
            int count = table.insert(account, entry);
            if (count == 1) {
                // 成功
                ret_code = 0;
            } else {
                // 失败? 无权限或者其他错误
                ret_code = -2;
            }
        } else {
            // 账户已存在
            ret_code = -1;
        }

        emit RegisterEvent(ret_code, account, asset_value);

        return ret_code;
    }
    function assetTransfer(string from_account, string to_account, uint256 amount) public returns(int) {
        // 查询转移资产账户信息
        int ret_code = 0;
        int256 ret = 0;
        uint256 from_asset_value = 0;
        uint256 to_asset_value = 0;
        
        // 转移账户是否存在?
        (ret, from_asset_value) = assetSelect(from_account);
        if(ret != 0) {
            ret_code = -1;
            // 转移账户不存在
            emit TransferEvent(ret_code, from_account, to_account, amount);
            return ret_code;

        }

        // 接受账户是否存在?
        (ret, to_asset_value) = assetSelect(to_account);
        if(ret != 0) {
            ret_code = -2;
            // 接收资产的账户不存在
            emit TransferEvent(ret_code, from_account, to_account, amount);
            return ret_code;
        }

        if(from_asset_value < amount) {
            ret_code = -3;
            // 转移资产的账户金额不足
            emit TransferEvent(ret_code, from_account, to_account, amount);
            return ret_code;
        } 

        if (to_asset_value + amount < to_asset_value) {
            ret_code = -4;
            // 接收账户金额溢出
            emit TransferEvent(ret_code, from_account, to_account, amount);
            return ret_code;
        }

        Table table = assetOpenTable();

        Entry entry0 = table.newEntry();
        entry0.set("account", from_account);
        entry0.set("asset_value", int256(from_asset_value - amount));
        // 更新转账账户
        int count = table.update(from_account, entry0, table.newCondition());
        if(count != 1) {
            ret_code = -5;
            // 失败? 无权限或者其他错误?
            emit TransferEvent(ret_code, from_account, to_account, amount);
            return ret_code;
        }

        Entry entry1 = table.newEntry();
        entry1.set("account", to_account);
        entry1.set("asset_value", int256(to_asset_value + amount));
        // 更新接收账户
        table.update(to_account, entry1, table.newCondition());

        emit TransferEvent(ret_code, from_account, to_account, amount);

        return ret_code;
    }
    
    
    //更新id
    function updateId(int256 payIdAccount) private returns(int) {
        TableFactory tr = TableFactory(0x1001);
        Table table = tr.openTable("pay_bill_id");
        
        Entry entry = table.newEntry();
        entry.set("pay_bill_id_name_id",payIdAccount);
        
        Condition condition = table.newCondition();
        condition.EQ("pay_bill_id_name","pay_bill");
        
        int count = table.update("pay_bill",entry,condition);
        
        return count;
    }
    //创建id数值
    function createId() private returns(int) {
        TableFactory tr = TableFactory(0x1001);
        Table table = tr.openTable("pay_bill_id");
        
        Entry entry = table.newEntry();
        entry.set("pay_bill_id_name","pay_bill");
        entry.set("pay_bill_id_name_id",int256(1));
        
        int count = table.insert("pay_bill",entry);
        return count;
    }
    //得到id值
    function getIdAccount() private returns(int256) {
        TableFactory tr = TableFactory(0x1001);
        Table table = tr.openTable("pay_bill_id");
        
        Condition condition = table.newCondition();
        condition.EQ("pay_bill_id_name","pay_bill");
        
        Entries entries = table.select("pay_bill",condition);
        if(entries.size() == 0) {
            int code = createId();
            if(code == 0) {return 0;}
            payid = 1;
            return 1;
        }
        return entries.get(0).getInt("pay_bill_id_name_id");
    }
    //创建id数值表
    function createIdTable() private returns(int) {
        TableFactory tr = TableFactory(0x1001);
        //创建id数值表
        //表名：pay_bill_id   key：pay_bill_id_name     value：pay_bill_id_name_id
        //数据类型：               string                     int256
        int count = tr.createTable("pay_bill_id","pay_bill_id_name","pay_bill_id_name_id");
        return count;
    }
    
    function createTable() private returns(int) {
        TableFactory tr = TableFactory(0x1001);
        //创建表
        //表名：pay_bill   key： allname  value: pay_from,pay_to,pay_account,pay_id
        //数据类型                string(a)      string    string    uint256    int256   
        int count = tr.createTable("pay_bill","allname","pay_from,pay_to,pay_account,pay_id");
        return count;
    }
    
    function openTable() private returns(Table) {
        TableFactory tr = TableFactory(0x1001);
        Table table = tr.openTable("pay_bill");
        return table;
    }
    
    function dayToSecond(uint256 dayTime) private returns(uint) {
        return uint(dayTime) * 86400;
    }
    
    function compareString(string a, string b) private returns(bool) {
        if (bytes(a).length != bytes(b).length) {
            return false;
        }
        for(uint i = 0;i < bytes(b).length;i++) {
            if(bytes(a)[i] != bytes(b)[i]) {
                return false;
            }
        }
        return true;
    }
    
    function bytes32ToString(bytes32 num) private returns(string) {
        bytes memory bytesString = new bytes(32);
        uint charCount = 0;
        for (uint j = 0; j < 32; j++) {
            byte char = byte(bytes32(uint(num) * 2 ** (8 * j)));
            if (char != 0) {
                bytesString[charCount] = char;
                charCount++;
            }
        }
        bytes memory bytesStringTrimmed = new bytes(charCount);
        for (j = 0; j < charCount; j++) {
            bytesStringTrimmed[j] = bytesString[j];
        }
        return string(bytesStringTrimmed);
    }
    
    //查询账户能力
    function check(string account) returns(uint256,uint256) {
        int256 num;
        uint256 num2=0;
       (num,num2) = assetSelect(account);
       if(num == -1) {return (0,1);}
        
        bytes32[] memory pay_from_list;
        bytes32[] memory pay_to_list;
        uint256[] memory pay_account_list;
        (pay_from_list,pay_to_list,pay_account_list) = select("empty",account,0);
        
        for(uint256 i = 0;i < pay_account_list.length;i++) {
            num2 += pay_account_list[i];
        }
        return (num2,1);
    }
    
    //转移
    function changepill(string payToNew, int256 payId) returns(int256,uint256) {
        int num;
        uint256 num2;
        (num,num2) = assetSelect(payToNew);
        if(num == -1) {
            assetRegister(payToNew,0);
        }
        
        Table table = openTable();
        
        bytes32[] memory pay_from_list;
        bytes32[] memory pay_to_list;
        uint256[] memory pay_account_list;
        (pay_from_list,pay_to_list,pay_account_list) = select("empty","empty",payId);
        //没有此账单 0
        if(pay_from_list.length == 0) {return (0,1);}
        
        int256 count;
        uint256 uless; 
        (count,uless) = insert(bytes32ToString(pay_from_list[0]),payToNew,pay_account_list[0]);
        //添加新账单失败 -1
        if(count == 0) {return (-1,1);}
        
        (count,uless) = deletepill("empty","empty",0,payId);
        //删除旧账单失败 -2
        if(count == 0) {
            (count,uless) = deletepill(bytes32ToString(pay_from_list[0]),payToNew,pay_account_list[0],0);
            //删除旧账单失败且删除新账单也失败 -3
            if(count == 0) {return (-3,1);}
            return (-2,1);
        }
        return (1,1);
    }
    
    //查找id
    function selectForId(string payFrom, string payTo, uint256 payAccount) returns(int256,uint256) {
        Table table = openTable();
        
        Condition condition = table.newCondition();
        
        if(!compareString(payFrom,"empty")) {
            condition.EQ("pay_from",payFrom);
        }
        if(!compareString(payTo,"empty")) {condition.EQ("pay_to",payTo);}
        if(int256(payAccount) != 0){condition.EQ("pay_account",int256(payAccount));}
        
        Entries entries = table.select("all",condition);
        if(entries.size() == 0) return (0,1);
        return (entries.get(0).getInt("pay_id"),1);
    }
    
    //删除  id为0，则根据payfrom，payto，payaccount；否则根据id
    function deletepill(string payFrom,string payTo, uint256 payAccount, int256 payId) returns(int256,uint256) {
        Table table = openTable();
        Condition condition = table.newCondition();
        if(payId != 0) {
            condition.EQ("pay_id",payId);
        }else{
            int256 num;
            uint256 num1;
            (num,num1) = selectForId(payFrom,payTo,payAccount);
            condition.EQ("pay_id",num);
        }
        int256 count = table.remove("all",condition);
        return (count,1);
        
    }
    
    //查询   字符串无的输入empty    int类输入0
    function select(string payFrom,string payTo, int256 payId) public returns(bytes32[],bytes32[],uint256[]) {
        Table table = openTable();
        
        Condition condition = table.newCondition();
        
        if(!compareString(payFrom,"empty")) {
            condition.EQ("pay_from",payFrom);
        }
        if(!compareString(payTo,"empty")) {condition.EQ("pay_to",payTo);}
        if(payId != 0){condition.EQ("pay_id",payId);}
        
        Entries entries = table.select("all",condition);
        
        bytes32[] memory pay_from_list = new bytes32[](uint256(entries.size()));
        bytes32[] memory pay_to_list = new bytes32[](uint256(entries.size()));
        uint256[] memory pay_account_list = new uint256[](uint256(entries.size()));
        //int256[] memory pay_id_list = new int256[](uint256(entries.size()));
        
        for(int i = 0;i < entries.size();i++) {
            Entry entry = entries.get(i);
            
            pay_from_list[uint256(i)] = entry.getBytes32("pay_from");
            pay_to_list[uint256(i)] = entry.getBytes32("pay_to");
            pay_account_list[uint256(i)] = uint256(entry.getInt("pay_account"));
            //pay_id_list[uint256(i)] = int256(entry.getInt("pay_id"));
        }
        
        return (pay_from_list,pay_to_list,pay_account_list);
    }
    
    
    //插入
    function insert(string payFrom, string payTo, uint256 payAccount) returns(int256, uint256) {
        int num;
        uint256 num2;
        (num,num2) = assetSelect(payFrom);
        //出钱方没有账户 -1
        if(num == -1) {return (-1,1);}
        (num,num2) = assetSelect(payTo);
        if(num == -1) {
            assetRegister(payTo,0);
        }
        
        
        Table table = openTable();
        
        Entry entry = table.newEntry();
        entry.set("allname","all");
        entry.set("pay_from",payFrom);
        entry.set("pay_to",payTo);
        entry.set("pay_account",int256(payAccount));
        entry.set("pay_id",payid);
        
        int256 count = table.insert("all",entry);
        if(count == 1) {
            payid++;
            updateId(payid);
        }
        
        return (count,1);
    }
    
    
    
}