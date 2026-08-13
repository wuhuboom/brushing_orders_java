import re
from datetime import datetime
from pathlib import Path

import pymysql

CONFIG_PATH = (
    Path(__file__).resolve().parents[1]
    / "brushing-admin"
    / "src"
    / "main"
    / "resources"
    / "application-druid.yml"
)


def load_db_config():
    text = CONFIG_PATH.read_text(encoding="utf-8")
    url = re.search(r"url:\s*jdbc:mysql://([^:/]+):(\d+)/([^?]+)", text)
    user = re.search(r"username:\s*(\S+)", text)
    password = re.search(r"password:\s*(\S+)", text)
    if not (url and user and password):
        raise RuntimeError("Failed to parse database config from application-druid.yml")
    return {
        "host": url.group(1),
        "port": int(url.group(2)),
        "database": url.group(3),
        "user": user.group(1),
        "password": password.group(1),
        "charset": "utf8mb4",
    }


def get_default_level_id(cur):
    cur.execute("SELECT id FROM order_member_level ORDER BY id ASC LIMIT 1")
    row = cur.fetchone()
    if not row:
        raise RuntimeError("No member level found in order_member_level")
    return row[0]


def main():
    db = load_db_config()
    conn = pymysql.connect(**db)
    cur = conn.cursor()
    level_id = get_default_level_id(cur)

    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    ts = datetime.now().strftime("%Y%m%d%H%M%S")

    test_users = [
        {
            "username": f"test_real_wd_{ts}",
            "phone": f"188{ts[-8:]}",
            "is_real": "N",
            "remark": "测试真人提现用户",
        },
        {
            "username": f"test_fake_wd_{ts}",
            "phone": f"199{ts[-8:]}",
            "is_real": "Y",
            "remark": "测试假人提现用户",
        },
    ]

    user_ids = {}
    for u in test_users:
        cur.execute(
            """
            INSERT INTO order_member_user (
                username, phone, password, trade_password, parent_id, ancestors,
                balance, frozen_balance, total_balance, credit_score,
                account_status, trade_status, withdraw_status, real_name_status,
                is_real, remark, create_time, task_status, level_id
            ) VALUES (
                %s, %s, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',
                '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',
                0, '0', 10000.00, 0.00, 10000.00, 100,
                '0', '0', '0', '0', %s, %s, %s, '0', %s
            )
            """,
            (u["username"], u["phone"], u["is_real"], u["remark"], now, level_id),
        )
        user_ids[u["is_real"]] = cur.lastrowid
        print(f"created user {u['username']} id={cur.lastrowid} is_real={u['is_real']}")

    withdrawals = [
        {
            "code": f"WD-REAL-{ts}-01",
            "user_id": user_ids["N"],
            "amount": 100.00,
            "credited_amount": 95.00,
            "fee": 5.00,
            "status": "1",
            "withdraw_name": "真人测试A",
            "withdraw_address": "0xREALTESTADDRESS001",
            "withdraw_type": "USDT",
            "remark": "真人提现测试数据1",
        },
        {
            "code": f"WD-REAL-{ts}-02",
            "user_id": user_ids["N"],
            "amount": 200.00,
            "credited_amount": 190.00,
            "fee": 10.00,
            "status": "1",
            "withdraw_name": "真人测试B",
            "withdraw_address": "0xREALTESTADDRESS002",
            "withdraw_type": "USDT",
            "remark": "真人提现测试数据2",
        },
        {
            "code": f"WD-FAKE-{ts}-01",
            "user_id": user_ids["Y"],
            "amount": 300.00,
            "credited_amount": 285.00,
            "fee": 15.00,
            "status": "1",
            "withdraw_name": "假人测试A",
            "withdraw_address": "0xFAKETESTADDRESS001",
            "withdraw_type": "USDT",
            "remark": "假人提现测试数据1",
        },
        {
            "code": f"WD-FAKE-{ts}-02",
            "user_id": user_ids["Y"],
            "amount": 400.00,
            "credited_amount": 380.00,
            "fee": 20.00,
            "status": "1",
            "withdraw_name": "假人测试B",
            "withdraw_address": "0xFAKETESTADDRESS002",
            "withdraw_type": "USDT",
            "remark": "假人提现测试数据2",
        },
    ]

    for w in withdrawals:
        cur.execute(
            """
            INSERT INTO order_withdrawal (
                code, user_id, amount, credited_amount, fee,
                application_time, status, withdraw_name, withdraw_address,
                withdraw_type, remark
            ) VALUES (
                %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s
            )
            """,
            (
                w["code"],
                w["user_id"],
                w["amount"],
                w["credited_amount"],
                w["fee"],
                now,
                w["status"],
                w["withdraw_name"],
                w["withdraw_address"],
                w["withdraw_type"],
                w["remark"],
            ),
        )
        print(f"created withdrawal {w['code']} id={cur.lastrowid}")

    conn.commit()

    print("\n=== verify inserted data ===")
    cur.execute(
        """
        SELECT w.code, u.username, u.is_real, w.amount, w.remark
        FROM order_withdrawal w
        JOIN order_member_user u ON u.id = w.user_id
        WHERE w.code LIKE %s
        ORDER BY w.id
        """,
        (f"%-{ts}-%",),
    )
    for row in cur.fetchall():
        print(row)

    conn.close()
    print("\nDone.")


if __name__ == "__main__":
    main()
