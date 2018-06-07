from flask import Flask, render_template, flash, redirect, url_for, session, logging,request
#from data import Articles
from wtforms import Form,TextAreaField, StringField, PasswordField, validators
from passlib.hash import sha256_crypt
import pymysql
from functools import wraps

app = Flask(__name__)
#连接数据库
connection = pymysql.connect(host='127.0.0.1',
                             user='root',
                             password='123456',
                             db='myflaskapp',
                             charset='utf8',
                             cursorclass=pymysql.cursors.DictCursor)
                            #这里需要注意的是在取出数据的时候把原
                            # 组变成字典的形式


#Articles = Articles()
app.config['SECRET_KEY'] = 'secret123456'



#index
@app.route('/')
def index():
    return render_template('home.html')



#about
@app.route('/about')
def about():
    return render_template('about.html')


#article
@app.route('/articles')
def articles():
    # 使用 cursor() 方法创建一个游标对象 cursor
    cursor = connection.cursor()
    # 取得文章
    result = cursor.execute("SELECT * FROM articles")
    articles = cursor.fetchall()

    if result > 0:
        return render_template('articles.html', articles=articles)
    else:
        msg = '没有找到此文章'
        return render_template('articles.html', msg=msg)

    cursor.close()
    #这里传入数据


#single article
@app.route('/article/<string:id>/')
def article(id):
    # 使用 cursor() 方法创建一个游标对象 cursor
    cursor = connection.cursor()

    # 取得文章
    result = cursor.execute("SELECT * FROM articles where id = %s", [id])

    article = cursor.fetchone()

    return render_template('article.html', article=article)
    #设置文章的跳转页面


#register form class
class RegisterForm(Form):
    name = StringField('姓名', [validators.length(min=1, max=50)])
    username = StringField('用户名', [validators.length(min=4, max=25)])
    email = StringField('邮箱', [validators.length(min=6, max=50)])
    password = PasswordField('密码', [
        validators.DataRequired(),
        validators.EqualTo('confirm', message='密码不正确')
    ])
    confirm = PasswordField('确认密码')




#register
@app.route('/register', methods=['GET', 'POST'])
def register():
    form = RegisterForm(request.form)
    if request.method == 'POST' and form.validate():
        #这里使用了WTF表单
        name = form.name.data
        email = form.email.data
        username = form.username.data
        password = sha256_crypt.encrypt(str(form.password.data))

        # 使用 cursor() 方法创建一个游标对象 cursor
        cursor = connection.cursor()
        # 执行数据库添加语句
        cursor.execute("INSERT INTO users(name,email,username,password) values (%s, %s, %s, %s)",
                           (name, email, username, password))
        # 执行后关闭连接
        cursor.close()
        # 提交数据到数据库
        connection.commit()

        flash('你现在已经注册成功！！！欢迎登陆Ray的个人网站','success')
        redirect(url_for('login'))

    return render_template('register.html', form=form)


#用户注册
@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        username = request.form['username']
        password_candidate = request.form['password']
        #得到提交到表单的值,不再使用wtf表单
        # 使用 cursor() 方法创建一个游标对象 cursor
        cursor = connection.cursor()
        # 执行数据库查询语句
        result = cursor.execute("SELECT * FROM users where username = %s", [username])

        if result>0:
            #取得存的hash值
            data = cursor.fetchone()
            password = data['password']

            #比较密码
            if sha256_crypt.verify(password_candidate, password):
                session['logged_in'] = True
                session['username'] = username
                #验证成功，将用户名密码保存在session中

                flash('你现在已经登陆成功', 'success')
                return redirect(url_for('dashboard'))

            else:
                error = '非法登陆'
                return render_template('login.html', error=error)
        else:
            error = '用户无法找到'
            return render_template('login.html', error=error)
        cursor.close()


    return render_template('login.html')



#检查用户是否登陆
def is_logged_in(f):
    @wraps(f)
    def wrap(*args, **kwargs):
        if 'logged_in' in session:
            return f(*args, **kwargs)
        else:
            flash('没有授权，请重新登录', 'danger')
            return redirect(url_for('login'))
    return wrap


#logout
@app.route('/logout')
def logout():
    session.clear()
    flash('你已经成功退出登陆', 'success')
    return redirect(url_for('login'))


#Dashboard
@app.route('/dashboard')
@is_logged_in
def dashboard():
    # 使用 cursor() 方法创建一个游标对象 cursor
    cursor = connection.cursor()
    # 取得文章
    result = cursor.execute("SELECT * FROM articles")
    articles = cursor.fetchall()

    if result > 0:
        return render_template('dashboard.html', articles=articles)
    else:
        msg = '没有找到此文章'
        return render_template('dashboard.html', msg=msg)
    #关闭连接
    cursor.close()



#增加文章，同样使用wtform来实现
class ArticleForm(Form):
    title = StringField('标题', [validators.length(min=1, max=200)])
    body = TextAreaField('内容', [validators.length(min=30)])


#edit article
@app.route('/edit_article/<string:id>', methods=['POST', 'GET'])
@is_logged_in
def edit_article(id):

    # 使用 cursor() 方法创建一个游标对象 cursor
    cursor = connection.cursor()

    # 执行数据库查询语句
    cursor.execute("SELECT * FROM articles WHERE id = %s", [id])
    article = cursor.fetchone()

    #获取表单
    form = ArticleForm(request.form)

    #填充表单
    form.title.data = article['title']
    form.body.data = article['body']

    if request.method == 'POST' and form.validate():

        #注意这里提交的是用户request的请求
        title = request.form['title']
        body = request.form['body']

        # 使用 cursor() 方法创建一个游标对象 cursor
        cursor = connection.cursor()

        # 执行数据库更新语句
        cursor.execute("UPDATE artilces SET title=%s, body=%s WHERE id =%s", (title, body, id))

        # 提交数据到数据库
        connection.commit()
        # 执行后关闭连接
        cursor.close()
        # 关闭数据库连接
        connection.close()
        flash('文章更新成功', 'success')

        return redirect(url_for('dashboard'))
    return render_template('edit_article.html', form=form)

#删除文章
@app.route('/delete_article/<string:id>', methods=['POST'])
@is_logged_in
def delete_article(id):
    # 使用 cursor() 方法创建一个游标对象 cursor
    cursor = connection.cursor()

    # 执行数据库更新语句
    cursor.execute("DELETE FROM articles where id=%s", [id])

    # 提交数据到数据库
    connection.commit()
    # 执行后关闭连接
    cursor.close()

    flash('文章删除成功', 'success')

    return redirect(url_for('dashboard'))





#add article
@app.route('/add_article', methods=['POST', 'GET'])
@is_logged_in
def add_article():
    form = ArticleForm(request.form)
    if request.method == 'POST' and form.validate():
        title = form.title.data
        body = form.body.data
        # 使用 cursor() 方法创建一个游标对象 cursor
        cursor = connection.cursor()
        # 执行数据库查询语句
        cursor.execute("INSERT INTO articles(title,body,author)VALUES(%s, %s, %s)", (title, body, session['username']))
        # 提交数据到数据库
        connection.commit()
        # 执行后关闭连接
        cursor.close()
        # 关闭数据库连接
        connection.close()
        flash('文章创建成功', 'success')

        return redirect(url_for('dashboard'))
    return render_template('add_article.html', form=form)



if __name__ == '__main__':
    app.run(debug=True)
