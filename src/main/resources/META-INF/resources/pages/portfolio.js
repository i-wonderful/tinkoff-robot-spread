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
                balance: {
                    value: ""
                },
                balanceUsd: {
                    value: ""
                },
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
                    // console.log(response.data);
                    this.portfolio = response.data;
                })
                .catch(error => {
                    console.info(error);
                });
        }
    }, mounted() {
        this.getPortfolio();
    },

    template: `
      <div>
        <h1>{{title}}</h1>
        <div class="grid">
            <div class="col-3">Режим песочницы:</div>
            <div class="col-9"><b>{{portfolio.sandbox}}</b></div>
            <div class="col-3">Account Id:</div>
            <div class="col-9">{{portfolio.accountId}}</div>
            <div class="col-3">Баланс RUB:</div>
            <div class="col-9">{{portfolio.balance.value}}</div>
            <div class="col-3">Баланс USD:</div>
            <div class="col-9">{{portfolio.balanceUsd.value}}</div>
            <div class="col-3">Ожидаемая доходность:</div>
            <div class="col-9">{{portfolio.expectedYeld}}</div>
            <div class="col-3">Всего куплено акций на сумму:</div>
            <div class="col-9">{{portfolio.totalAmountShares.value}}</div>
            <div class="col-3">Акции:</div>
            <div class="col-9">
                <ul>
                    <li v-for='position in portfolio.positions'>
                        Figi:<b>{{ position.figi }}</b> <span></span> 
                        количество: {{ position.quantity}} 
                        доходность: {{position.expectedYield}}
                    </li>
                </ul>
            </div>
        </div>
        <button @click="getPortfolio">Обновить</button>
      </div>
    `,
};