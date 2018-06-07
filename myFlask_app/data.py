import json
def Articles():
    articles = [
        {
            'id': 1,
            'title': '讲座一',
            'body': '华东师范大学计算机与科学软件工程学院人工智能科技研讨会。',
            'author': '何积峰',
            'create_date': '18-06-15'
        },
        {
            'id': 2,
            'title': '讲座二',
            'body': '华东师范大学计算机与科学软件工程学院区块链技术研讨会。',
            'author': '钱卫宁',
            'create_date': '18-07-15'
        },
        {
            'id': 3,
            'title': '讲座三',
            'body': '华东师范大学计算机与科学软件工程学院大数据与分布式网络安全研讨会。',
            'author': '周傲英',
            'create_date': '18-08-15'
        }
    ]
    return articles