export default {
    name: 'Robot',
    setup() {
        const title = '123'
        return {title}
    },
    data() {
        return {
            isRun: false,
            logs: [],
            logOrders: [],
            eventBus: {},
            exchanges: []
        }
    },
    methods: {
        onStart() {
            axios.get("/strategy/start")
                .then(response => {
                    console.log(response);
                    this.isRun = true;
                })
                .catch(e => {
                    console.info(e);
                });
        },
        onStop() {
            axios.get("/strategy/stop")
                .then(response => {
                    console.log(response);
                    this.isRun = false;
                })
                .catch(e => {
                    console.info(e);
                });
        },
        onCancelAllOrders() {
            axios.get("/strategy/cancel-all-orders")
                .then(resp => {
                    this.getCurrentOrders();
                });

        },
        getCurrentOrders() {
            this.logOrders = [];
            axios.get("/account/orders")
                .then(response => {
                    var orders = response.data;
                    for (const order of orders) {
                        this.logOrders.push(order);
                    }
                })
        },

        // todo for testing
        oneTick() {
            axios.get("/vertx/process");
        }
    },
    mounted() {
        this.eventBus = new EventBus('/eventbus');
        this.eventBus.onopen = () => {
            console.log(">>> Open Event bus");
            this.eventBus.registerHandler('LOG', (error, message) => {
                this.logs.push(message.body);
            });
            this.eventBus.registerHandler('LOG_ORDER', (error, message) => {
                this.logOrders.push(message.body);
            });
        }

        axios.get("/account/exchanges")
            .then(response => {
                this.exchanges = response.data;
            });

        this.getCurrentOrders();
    }
    ,

    template: `
      
        <h1>{{title}}</h1>
        <div class="grid">
            <div class="col-3">Биржи:</div>
            <div class="col-9">
                <div v-for="(exc, index) in exchanges" >
                    <b style="padding-right: 10px">{{exc.name}}</b> 
                    <span v-if="exc.open">открыта</span>
                    <span v-else>закрыта, 
                        <span v-if="exc.tradingDay">откроется через {{exc.hoursBeforeOpen}} часов {{exc.minutesBeforeOpen}} минут</span>
                        <span v-else>выходной</span>
                    </span>
                    <br/>
                </div>
            </div>
        </div>  
        
        <div>
            <button @click="onStart" >Go</button>
            <button @click="oneTick">One tick</button>
            <button @click="onCancelAllOrders">Отменить все заявки</button>
            <button @click="onStop" v-if="isRun" >Stop</button>
        </div>
        
        <h5>Заявки</h5>
        <div>
            <table class="table">
                <thead>
                    <tr>
                        <th>Тикер</th>
                        <th>Статус</th>
                        <th>Тип</th>
                        <th>Цена</th>
                        <th>orderId</th>
                    </tr>
                </thead>
                <tbody>       
                    <tr v-for="(order, index) in logOrders" >
                        <td>{{order.ticker}}</td>
                        <td>{{order.status}}</td>
                        <td>{{order.direction}}</td>
                        <td>{{order.initialPrice}} {{order.currency}}</td>
                        <td>{{order.orderId}}</td>
                    </tr>
                </tbody>
            </table>
            
        </div>
        
        <h5>Основной лог</h5>
        <div class="log-panel">
            <div v-for="(log, index) in logs" >
                {{logs[index]}}
            </div>
        </div>
        

      
    `
}