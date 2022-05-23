export default {
    name: 'Home',

    setup() {
        const title = 'ешеду'
        return {title}
    },
    data() {
        return {
            settings: {
                sandboxMode: "",
                exchangeNames: "",
                strategySpreadPercent: "",
                checkBuyTickers: "",
                tokenSandbox: "",
                tokenReal: ""
            }
        }
    }, methods: {
        getSettings() {
            axios.get("/account/settings")
                .then((response) => {
                    this.settings = response.data;
                });
        }
    },
    mounted() {
        this.getSettings();
    },

    template: `
        <div>
            <p>
            <h5>Настройки общие</h5>
            <div class="row">
                <div class="col-3">Режим песочницы:</div>
                <div class="col-9">{{settings.sandboxMode}}</div>
            </div>
            <div class="row">    
                <div class="col-3">Биржи:</div>
                <div class="col-9">{{settings.exchangeNames}}</div>
            </div>
            <div class="row">       
                <div class="col-3">Токен песочницы:</div>
                <div class="col-9">{{settings.tokenSandbox}}</div>
            </div>
            <div class="row">     
                <div class="col-3">Токен реального счета:</div>
                <div class="col-9">{{settings.tokenReal}}</div>                
            </div>
            </p>
<!--            <p>-->
                <h5>Настройки стратегии</h5>
                <div class="row">
                    <div class="col-3">Минимальная величина спреда, % от цены:</div>
                    <div class="col-9">{{settings.strategySpreadPercent}}</div>
                </div>
                <div class="row">
                    <div class="col-3">Торговать акциями, ticker:</div>
                    <div class="col-9">{{settings.checkBuyTickers}}</div>
                </div>
<!--                <div>    -->
<!--                    <div class="col-3"></div>-->
<!--                    <div class="col-9"></div>-->
<!--                    <div class="col-3"></div>-->
<!--                    <div class="col-9"></div>-->
<!--                    <div class="col-3"></div>-->
<!--                    <div class="col-9"></div>-->
<!--                </div>-->
<!--            </p>-->
        </div>
    `,
};