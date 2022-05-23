export default {
    name: 'Portfolio',
    setup() {
        const title = "Портфолио";
        return {
            title
        };
    }, data() {
        return {
            portfolio: {
                accountId: "",
                sandbox: "",
                balanceRub: "",
                balanceUsd: "",
                totalAmountCurrencies: "",
                expectedYeld: "",
                totalAmountShares: {
                    value: ""
                },
                positions: []
            }
        }
    },
    methods: {
        getPortfolio() {
            axios.get("/account/portfolio")
                .then(response => {
                    this.portfolio = response.data;
                })
                .catch(error => {
                    console.info(error);
                });
        },
        onRecreateSandbox(){
            axios.get("/account/recreate-sandbox")
                .then(() => {
                    this.getPortfolio()
                })
        }
    }, mounted() {
        this.getPortfolio();
    },

    template: `
      <div>
        <h3>{{title}}</h3>
        <div class="row">
            <div class="col-3">Режим песочницы:</div>
            <div class="col-9"><b>{{portfolio.sandbox}}</b></div>
        </div>
        <div class="row">    
            <div class="col-3">Account Id:</div>
            <div class="col-9">{{portfolio.accountId}}</div>
        </div>
        <div class="row">    
            <div class="col-3">Баланс rub:</div>
            <div class="col-9">{{portfolio.balanceRub}}</div>
        </div>
        <div class="row">               
            <div class="col-3">Баланс usd:</div>
            <div class="col-9">{{portfolio.balanceUsd}}</div>
        </div>
        <div class="row">       
            <div class="col-3">Общая стоимость валют в портфеле, rub </div>
            <div class="col-9">{{portfolio.totalAmountCurrencies}}</div>
        </div>
        <div class="row">       
            <div class="col-3">Ожидаемая доходность:</div>
            <div class="col-9">{{portfolio.expectedYeld}}</div>
        </div>
        <div class="row">       
            <div class="col-3">Всего куплено акций на сумму:</div>
            <div class="col-9">{{portfolio.totalAmountShares.value}}</div>
        </div>
        
        <br/>
        <button @click="onRecreateSandbox" v-if="portfolio.sandbox">Пересоздать аккаунт</button>
        <button @click="getPortfolio">Обновить</button>
        <br/>  
                <h3>Акции</h3> 
                <table class="table">
                    <thead>
                        <tr>
                            <th>Тикер</th>
                            <th>Название</th>
                            <th>Количество</th>
                            <th>Средняя цена</th>
                            <th>Ожидаемая доходность</th>
                        </tr>
                    </thead>
                    <tbody>       
                        <tr v-for="position in portfolio.positions" >
                            <td>{{position.ticker}}</td>
                            <td>{{position.name}}</td>
                            <td>{{position.quantity}}</td>
                            <td>{{position.averagePrice}} {{position.currency}}</td>
                            <td>{{position.expectedYield}}</td>
                        </tr>
                    </tbody>
            </table>
        
      </div>
    `,
};