import React from 'react'
import * as PropTypes from 'prop-types';
import {Card, Icon, Image} from 'semantic-ui-react'
import HttpClient from "../httpClient";
import AdoptionRequestModal from "./adoption-request-modal";


export default class AnimalCard extends React.Component {

    render() {
        return (<Card>
                <Card.Content>
                    <Image
                        floated='right'
                        size='medium'
                        src={this.props.animal.avatarUrl}
                    />
                    <Card.Header>{this.props.animal.name}</Card.Header>
                    <Card.Meta>
                        <span className='date'>Rescued on {this.props.animal.rescueDate}</span>
                    </Card.Meta>
                    <Card.Description>
                        {this.props.animal.description}
                    </Card.Description>
                </Card.Content>
                <Card.Content extra>
                    <div>
                        <Icon name='user'/>
                        {this.props.animal.adoptionRequests.length} Pending Adopters
                    </div>
                    <div className='ui two buttons'>
                        {/*<ViewAdoptersModal adopters={this.props.animal.adoptionRequests}/>*/}
                        <AdoptionRequestModal animal={this.props.animal} httpClient={this.props.httpClient}/>
                    </div>
                </Card.Content>
            </Card>
        );
    }
}

AnimalCard.propTypes = {
    animal: PropTypes.shape({
        avatarUrl: PropTypes.string.isRequired,
        name: PropTypes.string.isRequired,
        rescueDate: PropTypes.string.isRequired,
        description: PropTypes.string.isRequired,
        adoptionRequests: PropTypes.arrayOf(PropTypes.shape({
            adopterName: PropTypes.string.isRequired,
            email: PropTypes.string,
            notes: PropTypes.string,
        })).isRequired
    }).isRequired,
    httpClient: PropTypes.instanceOf(HttpClient).isRequired,
};

